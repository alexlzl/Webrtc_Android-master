package com.android.webrtc;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnectionFactory.InitializationOptions;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class WebRtcActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "WebRtcActivity";

    private SurfaceViewRenderer localSurfaceView;
    private SurfaceViewRenderer remoteSurfaceView;
    /**
     * 在渲染视频时,可能需要使用EglBase类。EglBase 提供了OpenGL ES上下文，用于渲染视频到Android视图（如SurfaceViewRenderer或TextureViewRenderer）中
     */
    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private VideoTrack videoTrack;
    private AudioTrack audioTrack;
    private PeerConnection peerConnection;
    private WebSocketClient webSocketClient;
    private DataChannel channel;
    private RtcSdpObserver observer;
    private List<String> streamList;
    /**
     * 配置STUN穿透服务器  转发服务器
     */
    private List<PeerConnection.IceServer> iceServers;
    private EditText tvFromName;
    private EditText tvFrom;
    private EditText tvToName;
    private EditText tvTo;
    private Button btConnect;
    private Button btCall;
    private Button btVideo;
    private Button btAudio;
    private TextView tvIsCall;
    private Button btReCall;
    private Button btReFuse;
    private String[] permission = new String[]{Manifest.permission.CAMERA};
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg != null) {
                String obj = (String) msg.obj;
                if (!TextUtils.isEmpty(obj)) {
                    switch (obj) {
                        case Constant.OPEN:
                            startPeerConnection();
                            break;
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webrtc);
        localSurfaceView = findViewById(R.id.LocalSurfaceView);
        remoteSurfaceView = findViewById(R.id.RemoteSurfaceView);
        tvFromName = findViewById(R.id.tv_from_name);
        tvFrom = findViewById(R.id.tv_from);
        tvToName = findViewById(R.id.tv_to_name);
        tvTo = findViewById(R.id.tv_to);
        btConnect = findViewById(R.id.bt_connect);
        btCall = findViewById(R.id.bt_call);
        btVideo = findViewById(R.id.bt_call_video);
        btAudio = findViewById(R.id.bt_call_audio);
        tvIsCall = findViewById(R.id.tv_iscall);
        btReFuse = findViewById(R.id.bt_refuse);
        btReCall = findViewById(R.id.bt_recall);
        btConnect.setOnClickListener(this);
        btCall.setOnClickListener(this);
        btVideo.setOnClickListener(this);
        btAudio.setOnClickListener(this);
        btReCall.setOnClickListener(this);
        btReFuse.setOnClickListener(this);
        initPermission();
    }

    /**
     * 初始化相机权限
     */
    private void initPermission() {

        /**
         * Android 6.0以上动态申请权限
         */
        PermissionRequest permissionRequest = new PermissionRequest();
        permissionRequest.requestRuntimePermission(
                this,
                permission,
                new PermissionListener() {

                    @Override
                    public void onGranted() {

                    }

                    @Override
                    public void onDenied(List<String> deniedPermissions) {

                    }
                });
    }

    /**
     * 连接Websocket
     */
    private void connectionWebsocket() {
        try {
            webSocketClient = new WebSocketClient(URI.create(SharePreferences.getInstance(WebRtcActivity.this).getServerUrl())) {
                @Override
                public void onOpen(ServerHandshake handShakeData) {
                    setText("webSocket已连接");
                    Log.e(TAG, "onOpen == getHttpStatus ==" + handShakeData.getHttpStatus() + " getHttpStatusMessage == " + handShakeData.getHttpStatusMessage());
                    /**
                     * 注册当前账号信息
                     */
                    Model model = new Model(Constant.REGISTER, getFromName(), getFrom(), getToName(), getTo());
                    webSocketClient.send(new Gson().toJson(model));
                }

                @Override
                public void onMessage(String message) {
                    Log.e(TAG, "onMessage==" + message);
                    if (!TextUtils.isEmpty(message)) {
                        Model model = new Gson().fromJson(message, Model.class);
                        if (model != null) {
                            String messageType = model.getMessageType();
                            if (!TextUtils.isEmpty(messageType)) {
                                int isSucceed = model.getIsSucceed();
                                switch (messageType) {
                                    case Constant.REGISTER_RESPONSE:
                                        /**
                                         * 注册用户信息返回=========开始初始化P2P连接流程
                                         */
                                        if (isSucceed == Constant.RESPONSE_SUCCEED) {
                                            Message msg = new Message();
                                            msg.obj = Constant.OPEN;
                                            handler.sendMessage(msg);
                                            Log.e(TAG, "用户信息注册成功");
                                        } else if (isSucceed == Constant.RESPONSE_FAILURE) {
                                            Log.e(TAG, "用户信息注册失败，已经注册");
                                        }
                                        break;
                                    case Constant.CALL_RESPONSE:
                                        /**
                                         * 发起音视频通讯请求返回
                                         */
                                        if (isSucceed == Constant.RESPONSE_SUCCEED) {
                                            Log.e(TAG, "对方在线，开始创建sdp offer");
                                            createOffer();
                                        } else if (isSucceed == Constant.RESPONSE_FAILURE) {
                                            Log.e(TAG, "对方不在线，连接失败====无法音视频通讯");
                                        }
                                        break;
                                    case Constant.INCALL:
                                        /**
                                         * 有音视频通讯进入
                                         */
                                        isInCall();
                                        break;
                                    case Constant.INCALL_RESPONSE:
                                        if (isSucceed == Constant.RESPONSE_SUCCEED) {
                                            /**
                                             * 接通音视频通讯
                                             */
                                            createOffer();
                                            Log.e(TAG, "对方同意接听");
                                        } else if (isSucceed == Constant.RESPONSE_FAILURE) {
                                            Log.e(TAG, "对方拒绝接听");
                                        }
                                        break;
                                    case Constant.OFFER:
                                        /**
                                         * 收到对方offer sdp 远端设置 RemoteDescription 完毕后，就要创建 Answer
                                         */
                                        SessionDescription sessionDescriptionRemote = model.getSessionDescription();
                                        peerConnection.setRemoteDescription(observer, sessionDescriptionRemote);
                                        createAnswer();
                                        break;
                                    case Constant.CANDIDATE:
                                        /**
                                         * 收到服务端的Candidate
                                         */
                                        IceCandidate iceCandidate = model.getIceCandidate();
                                        if (iceCandidate != null) {
                                            peerConnection.addIceCandidate(iceCandidate);
                                        }
                                        break;
                                }
                            }
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    setText("已关闭");
                    Log.e(TAG, "onClose == code " + code + " reason == " + reason + " remote == " + remote);
                }

                @Override
                public void onError(Exception ex) {
                    setText("onError == " + ex.getMessage());
                    Log.e(TAG, "onError== " + ex.getMessage());
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "socket Exception : " + e.getMessage());
        }
    }


    /**
     * 开始webrtc P2P连接流程
     */
    private void  startPeerConnection() {
        /**
         * setFieldTrials我们可以让Android端的WebRTC启用某些试用特性
         * WebRTC-H264Simulcast
         * WebRTC-FlexFEC-03
         * WebRTC-FlexFEC-03-Advertised
         * WebRTC-IncreasedReceivebuffers
         * WebRTC-SupportVP9SVC
         * WebRTC-VP8-Forced-Fallback-Encoder-v2
         * WebRTC-Video-BalancedDegradation
         * WebRTC-SimulcastScreenshare
         *
         * 设置的格式一般为： 名称/Enabled(Disabled)/  。如果有设置值的，格式一般为：  名称/值/
         */
        InitializationOptions initializationOptions = InitializationOptions.builder(this).setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/").createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        /**
         * 创建EglBase对象
         */
        eglBase = EglBase.create();
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableEncryption = true;
        options.disableNetworkMonitor = true;
        peerConnectionFactory = PeerConnectionFactory.builder().setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext())).setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true)).setOptions(options).createPeerConnectionFactory();
        /**
         * 配置STUN穿透服务器  转发服务器
         */
        iceServers = new ArrayList<>();
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder(Constant.STUN).createIceServer();
        iceServers.add(iceServer);

        streamList = new ArrayList<>();
        /**
         * 创建连接即为创建 PeerConnection，PeerConnection 是 WebRTC 非常重要的一个东西，是多人音视频通话连接的关键
         */
        PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);

        PeerConnectionObserver peerConnectionObserver = getPeerConnectionObserver();
        peerConnection = peerConnectionFactory.createPeerConnection(configuration, peerConnectionObserver);

        /**
         *  DataChannel.Init 可配参数说明：
         *  ordered：是否保证顺序传输；
         *  maxRetransmitTimeMs：重传允许的最长时间；
         *  maxRetransmits：重传允许的最大次数；
         */
        DataChannel.Init init = new DataChannel.Init();
        if (peerConnection != null) {
            channel = peerConnection.createDataChannel(Constant.CHANNEL, init);
        }
        DataChannelObserver dataChannelObserver = new DataChannelObserver();
        peerConnectionObserver.setObserver(dataChannelObserver);
        initView();
        initSdpObserver();
    }

    /**
     * PeerConnectionObserver是用来监听这个连接中的事件的监听者，可以用来监听一些如数据的到达、流的增加或删除等事件
     *
     * @return
     */
    @NonNull
    private PeerConnectionObserver getPeerConnectionObserver() {
        return new PeerConnectionObserver() {
            /**
             * 当一个新的 IceCandidate 被发现时触发。
             * @param iceCandidate
             */
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                sendIceCandidate(iceCandidate);
            }

            /**
             * 当从远程的流发布时触发
             * @param mediaStream
             */
            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);

                Log.d(TAG, "渲染远端画面onAddStream : " + mediaStream.toString());
                /**
                 * 渲染远端视频画面
                 */
                List<VideoTrack> videoTracks = mediaStream.videoTracks;
                if (videoTracks != null && videoTracks.size() > 0) {
                    VideoTrack videoTrack = videoTracks.get(0);
                    if (videoTrack != null) {
                        videoTrack.addSink(remoteSurfaceView);
                    }
                }
                /**
                 * 渲染远端音频画面
                 */
                List<AudioTrack> audioTracks = mediaStream.audioTracks;
                if (audioTracks != null && audioTracks.size() > 0) {
                    AudioTrack audioTrack = audioTracks.get(0);
                    if (audioTrack != null) {
                        audioTrack.setVolume(Constant.VOLUME);
                    }
                }
            }
        };
    }

    /**
     * 发送Candidate到对端
     *
     * @param iceCandidate
     */
    private void sendIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG, "onIceCandidate : " + iceCandidate.sdp);
        Log.d(TAG, "onIceCandidate : sdpMid = " + iceCandidate.sdpMid + " sdpMLineIndex = " + iceCandidate.sdpMLineIndex);
        Model model = new Model(getFromName(), getFrom(), getToName(), getTo());
        model.setMessageType(Constant.CANDIDATE);
        model.setCandidate(iceCandidate);
        String text = new Gson().toJson(model);
        Log.d(TAG, "setIceCandidate : " + text);
        webSocketClient.send(text);
    }

    /**
     * 初始化view
     */
    private void initView() {
        initSurfaceView(localSurfaceView);
        initSurfaceView(remoteSurfaceView);
        startLocalVideoCapture(localSurfaceView);
        startLocalAudioCapture();
    }

    /**
     * 初始化iew
     *
     * @param surfaceViewRenderer
     */
    private void initSurfaceView(SurfaceViewRenderer surfaceViewRenderer) {
        surfaceViewRenderer.init(eglBase.getEglBaseContext(), null);
        surfaceViewRenderer.setMirror(true);
        surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        surfaceViewRenderer.setKeepScreenOn(true);
        surfaceViewRenderer.setZOrderMediaOverlay(true);
        surfaceViewRenderer.setEnableHardwareScaler(false);
    }


    /**
     * 创建本地视频流
     */
    private void startLocalVideoCapture(SurfaceViewRenderer localSurfaceView) {
        /**
         * 创建一个 VideoSource 来拿到 VideoCapturer 采集的数据:参数说明是否为屏幕录制采集
         */
        VideoSource videoSource = peerConnectionFactory.createVideoSource(true);
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), eglBase.getEglBaseContext());
        VideoCapturer videoCapturer = createVideoCapturer();
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(Constant.VIDEO_RESOLUTION_WIDTH, Constant.VIDEO_RESOLUTION_HEIGHT, Constant.VIDEO_FPS);
        /**
         * 创建在 WebRTC 的连接中能传输的 VideoTrack 数据
         * 无论是本地还是远端的视频渲染，都是通过 WebRTC 提供的 SurfaceViewRenderer （继承于 SurfaceView） 进行渲染的。
         * 视频的数据需要 VideoTrack 绑定一个 VideoSink 的实现然后将数据渲染到 SurfaceViewRenderer 中
         */
        videoTrack = peerConnectionFactory.createVideoTrack(Constant.VIDEO_TRACK_ID, videoSource);
        videoTrack.addSink(localSurfaceView);
        MediaStream localMediaStream = peerConnectionFactory.createLocalMediaStream(Constant.LOCAL_VIDEO_STREAM);
        localMediaStream.addTrack(videoTrack);
        peerConnection.addTrack(videoTrack, streamList);
        peerConnection.addStream(localMediaStream);
    }

    /**
     * 创建本地音频流
     */
    private void startLocalAudioCapture() {
        /**
         * 语音
         */
        MediaConstraints audioConstraints = new MediaConstraints();
        /**
         * 回声消除
         */
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        /**
         * 自动增益
         */
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        /**
         * 高音过滤
         */
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        /**
         * 噪音处理
         */
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        /**
         * AudioSource 则可直接得到音频采集数据  创建一个 AudioTrack 即可在 WebRTC 的连接中传输
         */
        AudioSource audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        audioTrack = peerConnectionFactory.createAudioTrack(Constant.AUDIO_TRACK_ID, audioSource);
        MediaStream localMediaStream = peerConnectionFactory.createLocalMediaStream(Constant.LOCAL_AUDIO_STREAM);
        localMediaStream.addTrack(audioTrack);
        audioTrack.setVolume(Constant.VOLUME);
        peerConnection.addTrack(audioTrack, streamList);
        peerConnection.addStream(localMediaStream);
    }

    /**
     * WebRTC 视频采集需要创建一个 VideoCapturer，WebRTC 提供了 CameraEnumerator 接口，分别有 Camera1Enumerator 和 Camera2Enumerator 两个实现，
     * 能够快速创建所需要的 VideoCapturer，通过Camera2Enumerator.isSupported 判断是否支持Camera2 来选择创建哪个 CameraEnumerator
     *
     * @return
     */
    private VideoCapturer createVideoCapturer() {
        if (Camera2Enumerator.isSupported(this)) {
            return createCameraCapture(new Camera2Enumerator(this));
        } else {
            return createCameraCapture(new Camera1Enumerator(true));
        }
    }

    private VideoCapturer createCameraCapture(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        /**
         * 先捕获前置摄像头
         */
        Log.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        /**
         *  前置摄像头未找到获取后置摄像头
         */
        Log.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    private void initSdpObserver() {
        observer = new RtcSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                /**
                 *  创建 sdp 成功=============将会话描述设置在本地
                 */
                peerConnection.setLocalDescription(this, sessionDescription);
                SessionDescription localDescription = peerConnection.getLocalDescription();
                SessionDescription.Type type = localDescription.type;
                Log.e(TAG, "获取SDP==========onCreateSuccess == " + " type == " + type);
                /**
                 * 接下来使用之前的WebSocket实例将offer发送给服务器
                 */
                if (type == SessionDescription.Type.OFFER) {
                    /**
                     * 发送SDP到服务器
                     */
                    sendSdpOffer(sessionDescription);
                } else if (type == SessionDescription.Type.ANSWER) {
                    /**
                     * 应答
                     */
                    answer(sessionDescription);
                } else if (type == SessionDescription.Type.PRANSWER) {
                    /**
                     * 再次应答
                     */

                }
            }
        };

    }

    /**
     * 使用 MediaConstraints 来指定视频的约束条件
     */
    private void createOffer() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createOffer(observer, mediaConstraints);
    }

    private void createAnswer() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createAnswer(observer, mediaConstraints);
    }


    /**
     * 应答
     *
     * @param sdpDescription
     */
    private void answer(SessionDescription sdpDescription) {
        Model model = new Model(getFromName(), getFrom(), getToName(), getTo());
        model.setMessageType(Constant.OFFER);
        model.setSessionDescription(sdpDescription);
        String text = new Gson().toJson(model);
        Log.e(TAG, " answer " + text);
        webSocketClient.send(text);
    }

    /**
     * 发送SDP到对端
     *
     * @param sdpDescription
     */
    private void sendSdpOffer(SessionDescription sdpDescription) {
        Model model = new Model(getFromName(), getFrom(), getToName(), getTo());
        model.setMessageType(Constant.OFFER);
        model.setSessionDescription(sdpDescription);
        String text = new Gson().toJson(model);
        Log.e(TAG, " 发送SDP到服务器========== " + text);
        webSocketClient.send(text);
    }


    /**
     * 发起音视频通讯
     */
    private void communicateRequest() {
        Model model = new Model(getFromName(), getFrom(), getToName(), getTo());
        model.setMessageType(Constant.CALL);
        String text = new Gson().toJson(model);
        Log.d(TAG, "发起音视频通讯======webSocket消息 : " + text);
        webSocketClient.send(text);
    }

    /**
     * 是否接听
     */
    private void isInCall() {
        tvIsCall.setText("收到音视频通话请求，是否接听?");
    }

    /**
     * 接听通讯
     */
    private void reCall() {
        Model model = new Model(getFromName(), getFrom(), getToName(), getTo());
        model.setMessageType(Constant.INCALL);
        model.setIsSucceed(Constant.RESPONSE_SUCCEED);
        String text = new Gson().toJson(model);
        Log.d(TAG, "接通通讯reCall : " + text);
        webSocketClient.send(text);
    }

    /**
     * 拒绝通讯
     */
    private void reFuse() {
        Model model = new Model(getFromName(), getFrom(), getToName(), getTo());
        model.setMessageType(Constant.INCALL);
        model.setIsSucceed(Constant.RESPONSE_FAILURE);
        String text = new Gson().toJson(model);
        Log.d(TAG, "reFuse : " + text);
        webSocketClient.send(text);
    }


    @NonNull
    private String getFromName() {
        return tvFromName.getText().toString();
    }

    @NonNull
    private String getToName() {
        return tvToName.getText().toString();
    }

    private String getTo() {
        return tvTo.getText().toString().trim();
    }

    private String getFrom() {
        return tvFrom.getText().toString().trim();
    }

    private void setText(final String st) {
        runOnUiThread(() -> btConnect.setText(st));
    }


    @Override
    protected void onPause() {
        super.onPause();
        close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }


    private void close() {
        if (peerConnection != null) {
            peerConnection.close();
        }
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        if (localSurfaceView != null) {
            localSurfaceView.release();
        }
        if (remoteSurfaceView != null) {
            remoteSurfaceView.release();
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.bt_connect:
                    connectionWebsocket();
                    break;
                case R.id.bt_call:
                    /**
                     * 发起音视频通讯
                     */
                    communicateRequest();
                    break;
                case R.id.bt_call_video:
                    /**
                     * 发起视频通讯
                     */
                    Toast.makeText(this, "视频", Toast.LENGTH_LONG).show();
                    break;
                case R.id.bt_call_audio:
                    /**
                     * 发起音频通讯
                     */
                    Toast.makeText(this, "音频", Toast.LENGTH_LONG).show();
                    break;
                case R.id.bt_refuse:
                    /**
                     * 拒绝通讯
                     */
                    reFuse();
                    break;
                case R.id.bt_recall:
                    /**
                     * 接通通讯
                     */
                    reCall();
                    break;
            }
        }
    }

    private void sendMessage(String message) {
        byte[] msg = message.getBytes();
        DataChannel.Buffer buffer = new DataChannel.Buffer(ByteBuffer.wrap(msg), false);
        channel.send(buffer);
    }
}
