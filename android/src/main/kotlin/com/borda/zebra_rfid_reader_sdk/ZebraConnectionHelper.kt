package com.borda.zebra_rfid_reader_sdk

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.borda.zebra_rfid_reader_sdk.utils.*
import com.zebra.rfid.api3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * ZebraConnectionHelper is a helper class for connecting to a Zebra RFID reader via Bluetooth
 * and managing the configuration of the device.
 *
 * @property context Application context.
 * @property tagFindHandler A lambda function to handle tag locationing events.
 */
class ZebraConnectionHelper(
    private var context: Context,
    private var tagHandlerEvent: TagDataEventHandler,
    private var tagFindHandler: TagDataEventHandler,
    private var readTagEvent: TagDataEventHandler,
) :
    ViewModel() {
    private var readers: Readers
    private var availableRFIDReaderList: ArrayList<ReaderDevice>? = null
    private var readerDevice: ReaderDevice? = null
    private var reader: RFIDReader? = null
    private var rfidEventHandler: RfidEventHandler? = null


    init {
        Log.d(LOG_TAG, "Creating reader for bluetooth connection")
        readers = Readers(context, ENUM_TRANSPORT.BLUETOOTH)
    }

    override fun onCleared() {
        dispose()
        super.onCleared()
    }

    /**
     * Connects to the RFID reader with the given user configuration.
     *
     * @param readerConfig User configuration.
     */
    @Synchronized
    fun connect(name: String, readerConfig: HashMap<String, Any>) {
        viewModelScope.launch(Dispatchers.IO) {


            ReaderResponse.setConnectionStatus(ConnectionStatus.connecting)
            ReaderResponse.setName(name)
            tagHandlerEvent.sendEvent(ReaderResponse.toJson())

            Log.d(LOG_TAG, "connect called! ADDRESS -> $name")
            if (reader != null && reader!!.isConnected) {
                Log.d(LOG_TAG, "Reader is not null, disconnecting")
                reader!!.disconnect()
            }
            try {
                clearConfiguration()
                Log.d(LOG_TAG, "Reader is created")
                availableRFIDReaderList = readers.GetAvailableRFIDReaderList()
                if (availableRFIDReaderList != null) {
                    Log.d(LOG_TAG, "Readers found: " + availableRFIDReaderList.toString())

                    if (availableRFIDReaderList!!.size != 0) {
                        /// get first reader from list
                        readerDevice =
                            availableRFIDReaderList!!.first { readerDevice ->
                                readerDevice.name == name
                            }

                        reader = readerDevice?.rfidReader

                        if (reader != null) {
                            Log.d(LOG_TAG, "Connecting to reader")
                            reader!!.connect()
                            Log.d(LOG_TAG, "Connected! : Device -> ${readerDevice!!.name}")

                            /// Configure the reader with the user configuration
                            configureReader(readerConfig)

                            /// Trigger battery status
                            reader!!.Config.getDeviceStatus(true, true, false)

                            ReaderResponse.setConnectionStatus(ConnectionStatus.connected)
                            tagHandlerEvent.sendEvent(ReaderResponse.toJson())

                        }
                    }
                }

            } catch (e: InvalidUsageException) {
                e.printStackTrace()
                Log.d(LOG_TAG, "CONNECTION FAILED 1 -> InvalidUsageException")
                ReaderResponse.setAsConnectionError()
                tagHandlerEvent.sendEvent(ReaderResponse.toJson())

            } catch (e: OperationFailureException) {
                if (e.results == RFIDResults.RFID_READER_REGION_NOT_CONFIGURED) {
                    setDefaultRegion("TUR", name, readerConfig)
                }

                e.printStackTrace()
                Log.d(LOG_TAG, "CONNECTION FAILED 2 ->  ${e.results}")
                ReaderResponse.setAsConnectionError()
                tagHandlerEvent.sendEvent(ReaderResponse.toJson())

            }
        }
    }


    /**
     * Finds the tag with the given tag ID.
     *
     * @param tag The tag ID to find.
     */
    @Synchronized
    fun findTheTag(tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (reader != null && reader!!.isConnected) {
                    Log.d(LOG_TAG, "findTheTag called!")
                    BordaHandheldTrigger.setMode(TriggerMode.TAG_LOCATIONING_PERFORM)
                    TagLocationingResponse.setTag(tag)
                    TagLocationingResponse.setAnyReaderConnected(true)
                    tagFindHandler.sendEvent(TagLocationingResponse.toJson())
                }
            } catch (e: InvalidUsageException) {
                e.printStackTrace()
                Log.d(LOG_TAG, "InvalidUsageException " + e.vendorMessage)
            } catch (e: OperationFailureException) {
                e.printStackTrace()
                Log.d(
                    LOG_TAG, "OperationFailureException " + e.vendorMessage
                )
            }
        }
    }

    /**
     * Stops finding the tag.
     */
    @Synchronized
    fun stopFindingTheTag() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (reader != null && reader!!.isConnected) {
                    Log.d(LOG_TAG, "stopFindingTag called!")
                    BordaHandheldTrigger.setMode(TriggerMode.INVENTORY_PERFORM)
                    TagLocationingResponse.reset()
                    tagFindHandler.sendEvent(TagLocationingResponse.toJson())
                }
            } catch (e: InvalidUsageException) {
                e.printStackTrace()
                Log.d(LOG_TAG, "InvalidUsageException " + e.vendorMessage)
            } catch (e: OperationFailureException) {
                e.printStackTrace()
                Log.d(
                    LOG_TAG, "OperationFailureException " + e.vendorMessage
                )
            }
        }
    }

    /**
     * Resets the RFID reader configuration and clears associated resources.
     */
    private fun clearConfiguration() {
        readers = Readers(context, ENUM_TRANSPORT.BLUETOOTH)
        availableRFIDReaderList = null
        readerDevice = null
        reader = null
        rfidEventHandler = null
    }

    /**
     * Disconnects the RFID reader and cleans up all resources.
     */
    @Synchronized
    fun disconnect() {
        try {
            reader!!.disconnect()

            ReaderResponse.reset()
            tagHandlerEvent.sendEvent(ReaderResponse.toJson())

        } catch (e: InvalidUsageException) {
            e.printStackTrace()
            Log.d(LOG_TAG, "InvalidUsageException " + e.vendorMessage)
        } catch (e: OperationFailureException) {
            e.printStackTrace()
            Log.d(
                LOG_TAG, "OperationFailureException " + e.vendorMessage
            )
        }
    }

    /**
     * Configures the reader.
     *
     * @param readerConfig User configuration.
     */
    @Synchronized
    private fun configureReader(readerConfig: HashMap<String, Any>) {
        if (reader!!.isConnected) {
            Log.d(TAG, "ConfigureReader " + reader!!.hostName)

            var antennaPower = 0

            antennaPower = if (readerConfig["antennaPower"] == null) {
                getMaxAntennaPower()
            } else {
                readerConfig["antennaPower"] as Int
            }

            var beeperVolume = readerConfig["beeperVolume"] as Int
            var isDynamicPowerEnable = readerConfig["isDynamicPowerEnable"] as Boolean

            val triggerInfo = TriggerInfo()
            triggerInfo.StartTrigger.triggerType = START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE
            triggerInfo.StopTrigger.triggerType = STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE
            try {

                rfidEventHandler = RfidEventHandler(reader!!, tagHandlerEvent, tagFindHandler, readTagEvent)
                reader!!.Events.addEventsListener(rfidEventHandler)
                reader!!.Events.setHandheldEvent(true)
                reader!!.Events.setTagReadEvent(true)
                reader!!.Events.setBatteryEvent(true)
                reader!!.Events.setReaderDisconnectEvent(true)
                reader!!.Events.setOperationEndSummaryEvent(true)
                reader!!.Events.setInventoryStartEvent(true)
                reader!!.Events.setInventoryStopEvent(true)
                reader!!.Events.setAttachTagDataWithReadEvent(false)
                reader!!.Events.setInfoEvent(true)
                reader!!.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true)
                reader!!.Config.startTrigger = triggerInfo.StartTrigger
                reader!!.Config.stopTrigger = triggerInfo.StopTrigger

                val s1SingulationControl = reader!!.Config.Antennas.getSingulationControl(1)
                s1SingulationControl.session = SESSION.SESSION_S0
                s1SingulationControl.Action.inventoryState = INVENTORY_STATE.INVENTORY_STATE_A
                s1SingulationControl.Action.slFlag = SL_FLAG.SL_ALL
                reader!!.Config.Antennas.setSingulationControl(1, s1SingulationControl)
                reader!!.Actions.PreFilters.deleteAll()

                setDynamicPower(isDynamicPowerEnable)
                setBeeperVolumeConfig(beeperVolume)
                setAntennaConfig(antennaPower)

            } catch (e: InvalidUsageException) {
                Log.d(LOG_TAG, "InvalidUsageException -> configureReader")
                e.printStackTrace()
            } catch (e: OperationFailureException) {
                Log.d(LOG_TAG, "OperationFailureException -> configureReader")
                e.printStackTrace()
            }
        }
    }

    /**
     * Sets the singulation.
     *
     * @param tagPopulation Tag population.
     */
    private fun setSingulation(tagPopulation: Short?) {
        try {
            val s1_singulationControl = reader!!.Config.Antennas.getSingulationControl(1)
            s1_singulationControl.session = SESSION.SESSION_S0
            s1_singulationControl.Action.inventoryState = INVENTORY_STATE.INVENTORY_STATE_A
            s1_singulationControl.Action.setPerformStateAwareSingulationAction(true)
            s1_singulationControl.Action.slFlag = SL_FLAG.SL_ALL

            // TODO: tagPopulation info is taken from users. If it is not provided, it is set to 100.
            s1_singulationControl.tagPopulation = tagPopulation ?: 100
            reader!!.Config.Antennas.setSingulationControl(1, s1_singulationControl)
        } catch (e: InvalidUsageException) {
            e.printStackTrace()
        } catch (e: OperationFailureException) {
            e.printStackTrace()
        }
    }

    /**
     * Sets the default region.
     *
     * @param region The region code.
     * @param name The reader name.
     * @param readerConfig User configuration.
     */
    private fun setDefaultRegion(
        region: String,
        name: String,
        readerConfig: HashMap<String, Any>
    ) {
        try {
            if (reader != null) {
                val regulatoryConfig = reader!!.Config.regulatoryConfig
                val regions = reader!!.ReaderCapabilities.SupportedRegions
                val len = regions.length()
                for (i in 0 until len) {
                    val regionInfo = regions.getRegionInfo(i)

                    if (region == regionInfo.regionCode) {
                        val channels = arrayOf("865700", "866300", "866900", "867500")
                        regulatoryConfig.region = regionInfo.regionCode
                        regulatoryConfig.setEnabledChannels(channels)
                        regulatoryConfig.setIsHoppingOn(true)
                        reader!!.Config.regulatoryConfig = regulatoryConfig
                    }
                }
                connect(name, readerConfig)
            }
        } catch (e: InvalidUsageException) {
            e.printStackTrace()
            Log.d(LOG_TAG, "setDefaultRegion2 -> InvalidUsageException " + e.vendorMessage)
        } catch (e: OperationFailureException) {
            e.printStackTrace()
            Log.d(
                LOG_TAG, "setDefaultRegion2 -> OperationFailureException " + e.vendorMessage
            )
        }
    }

    /**
     * Retrieves the list of paired RFID readers.
     *
     * @return The list of paired RFID readers.
     */
    fun getAvailableReaderList(): java.util.ArrayList<ReaderDevice> {
        return readers.GetAvailableRFIDReaderList()
    }

    /**
     * Sets the antenna configuration.
     *
     * @param transmitPowerIndex Transmit power index.
     */
    fun setAntennaConfig(transmitPowerIndex: Int) {
        val antennaRfConfig = reader!!.Config.Antennas.getAntennaRfConfig(1)
        antennaRfConfig.setrfModeTableIndex(0)
        antennaRfConfig.tari = 0
        antennaRfConfig.transmitPowerIndex = transmitPowerIndex
        reader!!.Config.Antennas.setAntennaRfConfig(1, antennaRfConfig)
        setAntennaRange()
    }

    /**
     * Sets the antenna range.
     */

    private fun setAntennaRange() {
        var transmitPowerLevelValues = reader!!.ReaderCapabilities.transmitPowerLevelValues
        var antennaRange: IntArray =
            intArrayOf(transmitPowerLevelValues.first(), transmitPowerLevelValues.last())
        ReaderResponse.setAntennaRange(antennaRange)
        tagHandlerEvent.sendEvent(ReaderResponse.toJson())
    }

    private fun getMaxAntennaPower(): Int {
        var transmitPowerLevelValues = reader!!.ReaderCapabilities.transmitPowerLevelValues
        return transmitPowerLevelValues.last() - transmitPowerLevelValues.first()
    }

    /**
     * Sets the dynamic power optimization.
     *
     * @param setEnable Enable dynamic power optimization.
     */
    fun setDynamicPower(setEnable: Boolean) {
        reader!!.Config.dpoState =
            if (setEnable) DYNAMIC_POWER_OPTIMIZATION.ENABLE else DYNAMIC_POWER_OPTIMIZATION.DISABLE
    }

    /**
     * Sets the beeper volume configuration.
     *
     * @param level Beeper volume level.
     */
    fun setBeeperVolumeConfig(level: Int) {
        val config = when (level) {
            0 -> BEEPER_VOLUME.QUIET_BEEP
            1 -> BEEPER_VOLUME.LOW_BEEP
            2 -> BEEPER_VOLUME.MEDIUM_BEEP
            3 -> BEEPER_VOLUME.HIGH_BEEP
            else -> return
        }
        reader!!.Config.beeperVolume = config
    }

    /**
     * Disposes resources.
     */
    fun dispose() {
        try {
            if (reader != null) {
                reader!!.disconnect()
                reader = null
                readers.Dispose()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}


