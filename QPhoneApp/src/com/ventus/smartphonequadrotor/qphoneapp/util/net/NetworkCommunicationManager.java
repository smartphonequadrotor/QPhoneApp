package com.ventus.smartphonequadrotor.qphoneapp.util.net;

import com.google.gson.Gson;
import com.ventus.smartphonequadrotor.qphoneapp.activities.XmppConnectionActivity;
import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.services.MainService.MainServiceHandler;
import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.ControlLoop;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.Envelope;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.HeightSensorResponse;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.THrpyCommand;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.Responses;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.SystemState;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.TriAxisSensorResponse;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * This class provides a common interface for using the network communication to
 * interface with the controller. This class does not require the user to
 * differentiate between the Xmpp or Direct Socket methods for basic reads and
 * writes. Thus, once the initial setup is done, the users of this class can be
 * blissfully ignorant of the actual implementation details.
 * 
 * @author abhin
 * 
 */
public class NetworkCommunicationManager {
	public static final String TAG = NetworkCommunicationManager.class
			.getName();
	private MainService owner;

	private XmppClient xmppClient;
	private DirectSocketClient directSocketClient;

	private NcmOnMessageListener onMessageListener;
	private Gson gson;
	private NetworkCommunicationLooper networkCommunicationLooper;

	private String xmppOwnJid;
	private String xmppOwnPassword;
	private String xmppOwnResource;
	private String xmppTargetJid;

	/**
	 * This is the number of entries that will be cached in this class before an
	 * attempt to send this over the network is made.
	 */
	private static final int CACHE_SEND_THRESHOLD = 2;

	/**
	 * This is used to reduce the amount of kinematics data that is sent to the
	 * controller. For example, if this is 2 the every 2nd kinematics data point
	 * will be sent.
	 */
	private static final int CACHE_SKIP_COUNT_MAX = 5;
	private int kinematicsSkipCount = 0;
	private int accelerometerSkipCount = 0;
	private int gyroscopeSkipCount = 0;
	private int magnetometerSkipCount = 0;
	private int heightSkipCount = 0;

	/**
	 * The index where the next kinematics entry will be inserted into the
	 * cache.
	 */
	private int kinematicsBacklog = 0;
	private int accelerometerBacklog = 0;
	private int magnetometerBacklog = 0;
	private int gyroscopeBacklog = 0;
	private int heightBacklog = 0;

	/**
	 * An array of kinematics responses to slow down the rate at which network
	 * requests are made.
	 */
	private TriAxisSensorResponse[] kinematicsCache;
	private TriAxisSensorResponse[] accelerometerCache;
	private TriAxisSensorResponse[] gyroscopeCache;
	private TriAxisSensorResponse[] magnetometerCache;
	private HeightSensorResponse[] heightCache;

	/**
	 * In case both the connection clients ( {@link XmppClient} and
	 * {@link DirectSocketClient} ) are connected, then one of them will be
	 * chosen when sending the message using this variable.
	 */
	public CommunicationMethods preferredCommunicationMethod = CommunicationMethods.DIRECT_SOCKET;

	public NetworkCommunicationManager(MainService owner) {
		this.owner = owner;
		onMessageListener = new NcmOnMessageListener();
		gson = new Gson();
		kinematicsCache = new TriAxisSensorResponse[CACHE_SEND_THRESHOLD];
		accelerometerCache = new TriAxisSensorResponse[CACHE_SEND_THRESHOLD];
		magnetometerCache = new TriAxisSensorResponse[CACHE_SEND_THRESHOLD];
		gyroscopeCache = new TriAxisSensorResponse[CACHE_SEND_THRESHOLD];
		heightCache = new HeightSensorResponse[CACHE_SEND_THRESHOLD];
		networkCommunicationLooper = new NetworkCommunicationLooper();
		networkCommunicationLooper.start();
	}

	public NetworkCommunicationManager(XmppClient xmppClient, MainService owner) {
		this(owner);
		this.xmppClient = xmppClient;
	}

	public NetworkCommunicationManager(DirectSocketClient directSocketClient,
			MainService owner) {
		this(owner);
		this.directSocketClient = directSocketClient;
	}

	/**
	 * This method should be called by the {@link MainService} when it is
	 * ending. This method will stop the networkCommunicationHandler;
	 */
	public void cleanup() {
		if (networkCommunicationLooper.handler != null
				&& networkCommunicationLooper.handler.getLooper() != null)
			networkCommunicationLooper.handler.getLooper().quit();
	}

	public void setXmppClient(XmppClient xmppClient) {
		this.xmppClient = xmppClient;
	}

	public void setDirectSocketClient(DirectSocketClient directSocketClient) {
		this.directSocketClient = directSocketClient;
	}

	public String getXmppOwnJid() {
		return xmppOwnJid;
	}

	public void setXmppOwnJid(String xmppOwnUsername) {
		this.xmppOwnJid = xmppOwnUsername;
	}

	public String getXmppOwnPassword() {
		return xmppOwnPassword;
	}

	public void setXmppOwnPassword(String xmppOwnPassword) {
		this.xmppOwnPassword = xmppOwnPassword;
	}

	public String getXmppOwnResource() {
		return xmppOwnResource;
	}

	public void setXmppOwnResource(String xmppOwnResource) {
		this.xmppOwnResource = xmppOwnResource;
	}

	public String getXmppTargetJid() {
		return xmppTargetJid;
	}

	public void setXmppTargetJid(String xmppTargetJid) {
		this.xmppTargetJid = xmppTargetJid;
	}

	/**
	 * This method chooses the best communication method available out of
	 * {@link XmppClient} and {@link DirectSocketClient} based on the value of
	 * preferredCommunicationMethod and whether the clients of this instance
	 * have been set by the caller.
	 * 
	 * @throws Exception
	 */
	public void connect() throws Exception {
		if (xmppClient == null && directSocketClient == null) {
			throw new Exception(
					"Un-initialized communication clients; please initialize one");
		} else if (xmppClient != null
				&& (directSocketClient == null || preferredCommunicationMethod == CommunicationMethods.XMPP)) {
			xmppClient.connect();
			xmppClient.startSession(xmppOwnJid, xmppOwnPassword,
					xmppOwnResource, xmppTargetJid);
			xmppClient.onMessageListener = onMessageListener;
		} else {
			// TODO directSocketClient.connect();
		}
	}

	/**
	 * This class serves as the listener for the
	 * {@link NetworkCommunicationManager} when either the {@link XmppClient} or
	 * the {@link DirectSocketClient} receive a message over the network.
	 * 
	 * @author Abhin
	 * 
	 */
	public class NcmOnMessageListener extends OnMessageListener {
		@Override
		public void onMessage(String message) {
			// attempt to decode the message
			Envelope envelope = gson.fromJson(message, Envelope.class);
			if (envelope != null && envelope.getCommands() != null) {
				if (envelope.getCommands().getMoveCommandArray() != null
						&& envelope.getCommands().getMoveCommandArray().length > 0) {
					// send a message to the control loop
					owner.getControlLoop()
							.getDataAggregator()
							.processMoveCommand(
									envelope.getCommands()
											.getMoveCommandArray());
				}
				if (envelope.getCommands().getSystemState() != null
						&& !envelope.getCommands().getSystemState().equals("")) {
					if (envelope.getCommands().getSystemState()
							.equals(SystemState.ARMED.toString())) {
						// the user wishes to arm the quadrotor
						owner.sendFlightModeToQcb(true);
					} else if (envelope.getCommands().getSystemState()
							.equals(SystemState.CALIBRATING.toString())) {
						// the user wishes to calibrate the quadrotor
						owner.sendCalibrateSignalToQcb(true);
					} else if (envelope.getCommands().getSystemState()
							.equals(SystemState.ALTITUDE_HOLD_ENABLE.toString())){
						owner.sendAltitudeHoldToQcb(true);
					} else if (envelope.getCommands().getSystemState()
							.equals(SystemState.ALTITUDE_HOLD_DISABLE.toString())){
						owner.sendAltitudeHoldToQcb(false);
					}
				}
				if (envelope.getCommands().getHrpyCommandArray() != null
						&& envelope.getCommands().getHrpyCommandArray().length > 0) {
					// use the last element of the array and send that to the
					// QCB
					THrpyCommand[] desiredHrpyArray = envelope.getCommands()
							.getHrpyCommandArray();
					THrpyCommand desiredTHrpy = desiredHrpyArray[desiredHrpyArray.length - 1];
					owner.sendDesiredTHrpyToQcb(desiredTHrpy.getThrottle(),
							desiredTHrpy.getHeight(),
							desiredTHrpy.getRoll(), desiredTHrpy.getPitch(),
							desiredTHrpy.getYaw());
				}
				if (envelope.getCommands().getDebug() != null
						&& envelope.getCommands().getDebug().length != 0) {
					for (String debugStr : envelope.getCommands().getDebug()) {
						owner.sendDebugStringToQcb(debugStr);
					}
				}
			}
		}
	}

	/**
	 * This method is used by the {@link MainService} when it receives an intent
	 * from the activity requesting a connection between the smartphone and the
	 * controller through XMPP.
	 * 
	 * @param intent
	 *            The intent that contains the connection data.
	 * @throws Exception
	 */
	public void setupXmppConnection(Intent intent, Context context) {
		this.xmppClient = new XmppClient(
				intent.getStringExtra(IntentHandler.ActionExtras.SERVER_ADDRESS.extra),
				Integer.parseInt(intent
						.getStringExtra(IntentHandler.ActionExtras.SERVER_PORT.extra)),
				intent.getStringExtra(
						IntentHandler.ActionExtras.XMPP_OWN_JID.extra).split(
						"@")[1]);
		this.xmppOwnJid = intent
				.getStringExtra(IntentHandler.ActionExtras.XMPP_OWN_JID.extra);
		this.xmppOwnPassword = intent
				.getStringExtra(IntentHandler.ActionExtras.XMPP_OWN_PASSWORD.extra);
		this.xmppOwnResource = intent
				.getStringExtra(IntentHandler.ActionExtras.XMPP_OWN_RESOURCE.extra);
		this.xmppTargetJid = intent
				.getStringExtra(IntentHandler.ActionExtras.XMPP_TARGET_JID.extra);
		this.preferredCommunicationMethod = CommunicationMethods.XMPP;
		networkCommunicationLooper.handler.post(new Runnable() {
			public void run() {
				try {
					connect();
					sendConnectionSuccess();
				} catch (Exception e) {
					Log.e(TAG, "Could not connect to xmpp server", e);
					sendConnectionFailure();
				}
			}
		});
	}

	/**
	 * This method is used by the {@link MainService} when it receives an intent
	 * from the activity requesting a connection between the smartphone and the
	 * controller through XMPP.
	 * 
	 * @param intent
	 *            The intent that contains the connection data.
	 * @throws Exception
	 */
	public void setupDirectSocketConnection(Intent intent, Context context) {
		// TODO Auto-generated method stub

	}

	/**
	 * This method is used by the {@link MainService} when it receives an intent
	 * (through {@link IntentHandler}). This class is obviously used to send a
	 * message to the controller once a connection has been established. It uses
	 * the best connection between the 2 options depending on the value of the
	 * preferredCommunicationMethod variable.
	 * 
	 * @param message
	 *            The message to be sent
	 * @throws Exception
	 *             If neither direct socket nor xmpp connections are setup
	 */
	public void sendNetworkMessage(String message) throws Exception {
		if (xmppClient == null && directSocketClient == null) {
			throw new Exception(
					"Un-initialized communication clients; please initialize one");
		} else if (xmppClient != null
				&& (directSocketClient == null || preferredCommunicationMethod == CommunicationMethods.XMPP)) {
			xmppClient.sendMessage(message);
		} else {
			// TODO directSocketClient.sendMessage(message);
		}
	}

	/**
	 * This method is used by the {@link MainService} when it receives an intent
	 * (through {@link IntentHandler}). This class is obviously used to send a
	 * message to the controller once a connection has been established. It uses
	 * the best connection between the 2 options depending on the value of the
	 * preferredCommunicationMethod variable.
	 * 
	 * @param message
	 *            The message to be sent
	 * @throws Exception
	 *             If neither direct socket nor xmpp connections are setup
	 */
	public void sendNetworkMessage(Envelope envelope) throws Exception {
		sendNetworkMessage(gson.toJson(envelope));
	}

	private void sendConnectionFailure() {
		Intent intent = new Intent(
				XmppConnectionActivity.NETWORK_CONNECTION_STATUS_UPDATE);
		intent.putExtra(XmppConnectionActivity.NETWORK_CONNECTION_STATUS,
				XmppConnectionActivity.NETWORK_STATUS_CONNECTION_FAILURE);
		owner.sendBroadcast(intent);
	}

	private void sendConnectionSuccess() {
		Intent intent = new Intent(
				XmppConnectionActivity.NETWORK_CONNECTION_STATUS_UPDATE);
		intent.putExtra(XmppConnectionActivity.NETWORK_CONNECTION_STATUS,
				XmppConnectionActivity.NETWORK_STATUS_CONNECTED);
		owner.sendBroadcast(intent);
	}

	/**
	 * Sends the system state to the controller over the network.
	 * 
	 * @param state
	 *            the state of the quadrotor.
	 */
	public void sendSystemState(final SystemState state) {
		networkCommunicationLooper.handler.post(new Runnable() {
			public void run() {
				Responses responses = new Responses(null, null, null, null,
						null, null, null, null, state.toString(), null);
				Envelope envelope = new Envelope(null, null, responses);
				try {
					sendNetworkMessage(envelope);
				} catch (Exception e) {
					String errorStr = "Network failure: could not send system status";
					Message msg = new Message();
					msg.what = MainServiceHandler.TOAST_MESSAGE;
					msg.obj = errorStr;
					msg.arg1 = Toast.LENGTH_LONG;
					owner.handler.sendMessage(msg);
					Log.e(TAG, errorStr, e);
				}
			}
		});
	}

	public void sendKinematicsData(final long timestamp, final float roll,
			final float pitch, final float yaw) {
		// to make sure that the QCB can't overwhelm the phone application with
		// kinematics data
		if (networkCommunicationLooper.handler
				.hasMessages(NetworkCommunicationLooper.KINEMATICS_MESSAGE)) {
			networkCommunicationLooper.handler
					.removeMessages(NetworkCommunicationLooper.KINEMATICS_MESSAGE);
		}
		Message msg = networkCommunicationLooper.handler
				.obtainMessage(NetworkCommunicationLooper.KINEMATICS_MESSAGE);
		msg.obj = new Runnable() {
			public void run() {
				kinematicsSkipCount++;
				if (kinematicsSkipCount == CACHE_SKIP_COUNT_MAX) {
					kinematicsSkipCount = 0;
					assert (kinematicsBacklog < CACHE_SEND_THRESHOLD);
					kinematicsCache[kinematicsBacklog] = new TriAxisSensorResponse(
							timestamp, roll, pitch, yaw);
					kinematicsBacklog++;
					if (kinematicsBacklog >= CACHE_SEND_THRESHOLD) {
						Responses responses = new Responses(kinematicsCache,
								null, null, null, null, null, null, null, null,
								null);
						Envelope envelope = new Envelope(null, null, responses);
						try {
							sendNetworkMessage(envelope);
							// if the send is successful, then the cache should
							// be cleared
							kinematicsBacklog = 0;
						} catch (Exception e) {
							// if the kinematics data could not be sent because
							// the network connection
							// has not yet been setup, then don't log anything
							if (xmppClient != null
									|| directSocketClient != null) {
								String errorStr = "Network failure: could not send system status";
								Message msg = new Message();
								msg.what = MainServiceHandler.TOAST_MESSAGE;
								msg.obj = errorStr;
								msg.arg1 = Toast.LENGTH_LONG;
								owner.handler.sendMessage(msg);
								Log.e(TAG, errorStr, e);
							}
							// if the kinematicsCache is completely full, then
							// this is a cache overflow
							// empty the cache
							if (kinematicsBacklog == CACHE_SEND_THRESHOLD)
								kinematicsBacklog = 0;
						}
					}
				}
			}
		};
		networkCommunicationLooper.handler.sendMessage(msg);
	}

	public void sendAccelerometerData(final long timestamp, final float x,
			final float y, final float z) {
		// to make sure that the QCB can't overwhelm the phone application with
		// data
		if (networkCommunicationLooper.handler
				.hasMessages(NetworkCommunicationLooper.ACCELEROMETER_MESSAGE)) {
			networkCommunicationLooper.handler
					.removeMessages(NetworkCommunicationLooper.ACCELEROMETER_MESSAGE);
		}
		Message msg = networkCommunicationLooper.handler
				.obtainMessage(NetworkCommunicationLooper.ACCELEROMETER_MESSAGE);
		msg.obj = new Runnable() {
			public void run() {
				accelerometerSkipCount++;
				if (accelerometerSkipCount == CACHE_SKIP_COUNT_MAX) {
					accelerometerSkipCount = 0;
					assert (accelerometerBacklog < CACHE_SEND_THRESHOLD);
					accelerometerCache[accelerometerBacklog] = new TriAxisSensorResponse(
							timestamp, x, y, z);
					accelerometerBacklog++;
					if (accelerometerBacklog >= CACHE_SEND_THRESHOLD) {
						Responses responses = new Responses(null, null,
								accelerometerCache, null, null, null, null,
								null, null, null);
						Envelope envelope = new Envelope(null, null, responses);
						try {
							sendNetworkMessage(envelope);
							// if the send is successful, then the cache should
							// be cleared
							accelerometerBacklog = 0;
						} catch (Exception e) {
							// if the data could not be sent because the network
							// connection
							// has not yet been setup, then don't log anything
							if (xmppClient != null
									|| directSocketClient != null) {
								String errorStr = "Network failure: could not send system status";
								Message msg = new Message();
								msg.what = MainServiceHandler.TOAST_MESSAGE;
								msg.obj = errorStr;
								msg.arg1 = Toast.LENGTH_LONG;
								owner.handler.sendMessage(msg);
								Log.e(TAG, errorStr, e);
							}
							// if the cache is completely full, then this is a
							// cache overflow
							// empty the cache
							if (accelerometerBacklog == CACHE_SEND_THRESHOLD)
								accelerometerBacklog = 0;
						}
					}
				}
			}
		};
		networkCommunicationLooper.handler.sendMessage(msg);
	}

	public void sendMagnetometerData(final long timestamp, final float x,
			final float y, final float z) {
		// to make sure that the QCB can't overwhelm the phone application with
		// data
		if (networkCommunicationLooper.handler
				.hasMessages(NetworkCommunicationLooper.MAGNETOMETER_MESSAGE)) {
			networkCommunicationLooper.handler
					.removeMessages(NetworkCommunicationLooper.MAGNETOMETER_MESSAGE);
		}
		Message msg = networkCommunicationLooper.handler
				.obtainMessage(NetworkCommunicationLooper.MAGNETOMETER_MESSAGE);
		msg.obj = new Runnable() {
			public void run() {
				magnetometerSkipCount++;
				if (magnetometerSkipCount == CACHE_SKIP_COUNT_MAX) {
					magnetometerSkipCount = 0;
					assert (magnetometerBacklog < CACHE_SEND_THRESHOLD);
					magnetometerCache[magnetometerBacklog] = new TriAxisSensorResponse(
							timestamp, x, y, z);
					magnetometerBacklog++;
					if (magnetometerBacklog >= CACHE_SEND_THRESHOLD) {
						Responses responses = new Responses(null, null, null,
								magnetometerCache, null, null, null, null,
								null, null);
						Envelope envelope = new Envelope(null, null, responses);
						try {
							sendNetworkMessage(envelope);
							// if the send is successful, then the cache should
							// be cleared
							magnetometerBacklog = 0;
						} catch (Exception e) {
							// if the data could not be sent because the network
							// connection
							// has not yet been setup, then don't log anything
							if (xmppClient != null
									|| directSocketClient != null) {
								String errorStr = "Network failure: could not send system status";
								Message msg = new Message();
								msg.what = MainServiceHandler.TOAST_MESSAGE;
								msg.obj = errorStr;
								msg.arg1 = Toast.LENGTH_LONG;
								owner.handler.sendMessage(msg);
								Log.e(TAG, errorStr, e);
							}
							// if the cache is completely full, then this is a
							// cache overflow
							// empty the cache
							if (magnetometerBacklog == CACHE_SEND_THRESHOLD)
								magnetometerBacklog = 0;
						}
					}
				}
			}
		};
		networkCommunicationLooper.handler.sendMessage(msg);
	}

	public void sendGyroscopeData(final long timestamp, final float x,
			final float y, final float z) {
		// to make sure that the QCB can't overwhelm the phone application with
		// data
		if (networkCommunicationLooper.handler
				.hasMessages(NetworkCommunicationLooper.GYROSCOPE_MESSAGE)) {
			networkCommunicationLooper.handler
					.removeMessages(NetworkCommunicationLooper.GYROSCOPE_MESSAGE);
		}
		Message msg = networkCommunicationLooper.handler
				.obtainMessage(NetworkCommunicationLooper.GYROSCOPE_MESSAGE);
		msg.obj = new Runnable() {
			public void run() {
				gyroscopeSkipCount++;
				if (gyroscopeSkipCount == CACHE_SKIP_COUNT_MAX) {
					gyroscopeSkipCount = 0;
					assert (gyroscopeBacklog < CACHE_SEND_THRESHOLD);
					gyroscopeCache[gyroscopeBacklog] = new TriAxisSensorResponse(
							timestamp, x, y, z);
					gyroscopeBacklog++;
					if (gyroscopeBacklog >= CACHE_SEND_THRESHOLD) {
						Responses responses = new Responses(null,
								gyroscopeCache, null, null, null, null, null,
								null, null, null);
						Envelope envelope = new Envelope(null, null, responses);
						try {
							sendNetworkMessage(envelope);
							// if the send is successful, then the cache should
							// be cleared
							gyroscopeBacklog = 0;
						} catch (Exception e) {
							// if the data could not be sent because the network
							// connection
							// has not yet been setup, then don't log anything
							if (xmppClient != null
									|| directSocketClient != null) {
								String errorStr = "Network failure: could not send system status";
								Message msg = new Message();
								msg.what = MainServiceHandler.TOAST_MESSAGE;
								msg.obj = errorStr;
								msg.arg1 = Toast.LENGTH_LONG;
								owner.handler.sendMessage(msg);
								Log.e(TAG, errorStr, e);
							}
							// if the cache is completely full, then this is a
							// cache overflow
							// empty the cache
							if (gyroscopeBacklog == CACHE_SEND_THRESHOLD)
								gyroscopeBacklog = 0;
						}
					}
				}
			}
		};
		networkCommunicationLooper.handler.sendMessage(msg);
	}

	public void sendHeightData(final long timestamp, final int height) {
		// to make sure that the QCB can't overwhelm the phone application with
		// data
		if (networkCommunicationLooper.handler
				.hasMessages(NetworkCommunicationLooper.HEIGHT_MESSAGE)) {
			networkCommunicationLooper.handler
					.removeMessages(NetworkCommunicationLooper.HEIGHT_MESSAGE);
		}
		Message msg = networkCommunicationLooper.handler
				.obtainMessage(NetworkCommunicationLooper.HEIGHT_MESSAGE);
		msg.obj = new Runnable() {
			public void run() {
				heightSkipCount++;
				if (heightSkipCount == CACHE_SKIP_COUNT_MAX) {
					heightSkipCount = 0;
					assert (heightBacklog < CACHE_SEND_THRESHOLD);
					heightCache[heightBacklog] = new HeightSensorResponse(
							height, timestamp);
					heightBacklog++;
					if (heightBacklog >= CACHE_SEND_THRESHOLD) {
						Responses responses = new Responses(null, null, null,
								null, heightCache, null, null, null, null, null);
						Envelope envelope = new Envelope(null, null, responses);
						try {
							sendNetworkMessage(envelope);
							// if the send is successful, then the cache should
							// be cleared
							heightBacklog = 0;
						} catch (Exception e) {
							// if the data could not be sent because the network
							// connection
							// has not yet been setup, then don't log anything
							if (xmppClient != null
									|| directSocketClient != null) {
								String errorStr = "Network failure: could not send system status";
								Message msg = new Message();
								msg.what = MainServiceHandler.TOAST_MESSAGE;
								msg.obj = errorStr;
								msg.arg1 = Toast.LENGTH_LONG;
								owner.handler.sendMessage(msg);
								Log.e(TAG, errorStr, e);
							}
							// if the cache is completely full, then this is a
							// cache overflow
							// empty the cache
							if (heightBacklog == CACHE_SEND_THRESHOLD)
								heightBacklog = 0;
						}
					}
				}
			}
		};
		networkCommunicationLooper.handler.sendMessage(msg);
	}

	public class NetworkCommunicationLooper extends Thread {
		public static final int KINEMATICS_MESSAGE = 1;
		public static final int ACCELEROMETER_MESSAGE = 2;
		public static final int MAGNETOMETER_MESSAGE = 3;
		public static final int GYROSCOPE_MESSAGE = 4;
		public static final int HEIGHT_MESSAGE = 5;

		public Handler handler;

		public NetworkCommunicationLooper() {
			super("NetworkCommunicationLooper");
		}

		public void run() {
			Looper.prepare();
			handler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (msg.what == KINEMATICS_MESSAGE
							|| msg.what == ACCELEROMETER_MESSAGE
							|| msg.what == MAGNETOMETER_MESSAGE
							|| msg.what == GYROSCOPE_MESSAGE
							|| msg.what == HEIGHT_MESSAGE) {
						((Runnable) msg.obj).run();
					}
				}
			};
			Looper.loop();
		}
	}
}