# pure-live

## 项目架构

**前端 [pure-live-app](https://github.com/gnl00/pure-live-web)**

* `Vue.js`
* `VueRouter`
* `Vite`

**后端 [pure-live](https://github.com/gnl00/pure-live)**
* `SpringBoot`
* `SpringMVC`
* `Redis` 存储视频流
* `Nginx` 存储静态资源
* `WebSocket` 服务端将视频流推送给用户

**工具**
* ffmpeg


<br>

## 踩坑

1. 多数据源配置
2. 视频流录制与流拆分
3. 视频流格式
   前端/客户端上传格式，后端接收格式，后端响应格式，前端/客户端接受格式
4. Blob 二进制数据太大，Websocket 无法发送
5. Websocket 发送的连续 blob 流到前端如何播放
6. ...

<br>

### 视频流录制与拆分

<br>

#### 存在问题

在 Web 端，使用 `MediaRecorder` 来录制视频流信息

```javascript
const recordOptions = {
    /**
     * videoBitsPerSecond: 视频比特率，默认 2.5 Mbps => 2500000
     * 360p => 1 Mbps
     * 480p => 2.5 Mbps
     * 720p => 5 Mbps
     * 1080p => 8 Mbps
     * 1440p => 16 Mbps
     * 2160p => 35 ~ 45 Mbps
     */
    videoBitsPerSecond : 8000000,
    mimeType : 'video/webm;codecs=h264'
}

// 传入 stream 对象
const recorder = recorderGlobal = new MediaRecorder(stream, recordOptions);
recorder.start() // 开始录制视频
// 需要在 ondataavailable 方法中处理视频数据
recorder.ondataavailable = evt => handleRecordData(evt)
recorder.onstop = evt => onRecorderStop(evt)

// some where
recorder.stop()
// getthering data...

const chunks = []
const handleRecordData = evt => {
    chunks.push(evt.data)
}

```
但是 `ondataavailable` 方法只有在执行 `recorder.stop()` 后才触发。矛盾在于：`recorder.stop()` 是用户主动触发，
系统并不知道，可能造成 chunks 溢出。

<br>

#### 解决办法

阅读 [MDN-MediaRecorder](https://developer.mozilla.org/zh-CN/docs/Web/API/MediaRecorder/dataavailable_event) 发现，ondataavailable 方法的触发方式有以下几种：
* 媒体流结束时
* 当调用 `MediaRecorder.stop()` 时
* 调用 `MediaRecorder.requestData()` 时，将传递自记录开始或事件最后一次发生以来捕获的所有媒体数据；然后 Blob 创建一个新文件，并将媒体捕获继续到该 blob 中
* 如果将 timeslice 属性传递到开始媒体捕获的 `MediaRecorder.start()` 方法中，则每 timeslice 毫秒触发一次事件

最终决定采用 `MediaRecorder.start(timeslice)` 方法，基于以下思考
* 可以将 timeslice 设置得尽量少，减少用户视频延迟感知，更加简单灵活
* 如果改成限制 chunk 大小，在延迟控制方面没有那么好控制。当然也可以将 chunk 限制得足够小，以此来减少延迟
* 最终情况还是需要经过测试决定

<br>

### 视频流格式

#### 视频流前端上传格式

`Blob` 是一种二进制数据类型，用于表示不可变的、原始数据（比如文本、图像、音频或视频数据）的片段。其名字来源于 Binary Large Object， 可以被看作是包含任意数据的“大二进制对象”。
将 Blob 对象从前端传到后端可以使用 2 种常见的方式：
* Base64 编码格式
* `ArrayBuffer` 原生字节格式

一般使用 FileReader 对象来读取 Blob 中的数据
```javascript
const reader = new FileReader();
reader.onload = () => {
    // 将 Blob 数据转换为 Base64 编码的字符串
    const base64String = btoa(reader.result);
    // 将 Blob 数据转换为 ArrayBuffer 格式的原始字节
    const arrayBuffer = reader.result;
    // 向后端发送包含 Blob 数据...
};
reader.readAsBinaryString(blob);
```

Base64 编码格式内容大概如下：
```text
GkXfo6NChoEBQveBAULygQRC84EIQoKIbWF0cm9za2FCh4EEQoWBAhhTgGcB/////////xVJqWaZKtexgw9CQE2AhkNocm9tZVdBhkNocm9tZRZUrmvArr7XgQFzxYcmH
```

ArrayBuffer 原生字节格式看起来就像一串乱码：
```text
Eß££BB÷BòBóBmatroskaBBSgÿÿÿÿÿÿÿI©f*×±B@MChromeWAChromeT®kÀ®¾×sÅ1GQÏ¶¿V_MPEG4/ISO/AVCà°8ºPU°U±U¹UºU»C¶uÿÿÿÿÿÿÿç£ Ç'B %òø``».à½ïáD(Î %¸@ÿÿQ@BÇvNOÿû h*rÙV)ÌqU FR~Ì+ÿø!øfEB¥
```

一般来说，使用哪种格式都可以，主要基于项目考虑。
因为当前项目依赖 WebSocket，使用 ArrayBuffer 格式的数据传输更直接高效。因为 WebSocket 是使用二进制进行传输的，使用 ArrayBuffer 格式可以避免中间的编码、解码过程，减少了传输开销和延迟。

<br>

#### 视频流后端接收

前端使用 ArrayBuffer 格式传输，后端可以使用 `byte[]` 来接收数据
```java
@PostMapping("/blobUpload")
public void blobUpload(@RequestBody byte[] blobs) {}
```

#### 视频流客户端接收格式

接下来的问题是：如何将 `byte[]` 数据传输给用户客户端，客户端接收到的数据会是什么类型，客户端又该如何处理。答案依次是：
<br>
1、以 byte[] 格式给前端提供响应
<br>
2、客户端使用 fetch API 来处理请求，接收到的格式是 `ReadableStream`
<br>
3、根据 [MDN: Fetch Response Body](https://developer.mozilla.org/zh-CN/docs/Web/API/Fetch_API/Using_Fetch#body) 描述，可以使用以下方法来接收响应数据
* `Response.text()`
* `Response.json()`
* `Response.arrayBuffer()`
* `Response.blob()`
* `Response.formData()`

注意：不管以何种方式读取响应流，都只能读取一次，即 `Response.xxx()` 方法只能调用一次，在读取过后应该将有效结果保存。

<br>

因此对于 `byte[]` 格式的响应体，可以用 `Response.arrayBuffer()` 来处理
```javascript
response.arrayBuffer().then(body => {
   const view = new Uint8Array(body);
   const blob = new Blob([view], {type: 'video/webm'})
   const objectURL = URL.createObjectURL(blob);
   videoObjectURL.value = objectURL
})
```

在前后端数据传输格式上卡了许久，终于解决了。Thanks GOD ! 数据传输流程如下：
1、前端/客户端 ==视频数据收集==视频数据转换成 Blob 格式，以 application/otcet-stream 格式发送给后端（不需要将 Blob 转换成 ArrayBuffer）
2、后端以 `byte[]` 格式接收前端传输的数据，并保存到 ByteBuffer 中
3、后端以 `byte[]` 格式响应前端请求
4、前端以 `Response.blob()` 方法接收

### Blob 数据太大
设置后端 WebSocket `@OnMessage(maxMessageSize = 5120000)` 即可

<br>

### 中间出现的问题
> **Update-1**
> 项目中使用 Websocket 从端到端传输 blob 数据，使用 URL.createObjectURL 来创建 blob 视频链接，保存在内存中。存在的问题是，如果有连续不断的 websocket 数据传输，
> 就需要使用 URL.createObjectURL 连续生成视频对象，后面的对象会将前面的对象覆盖。
>
> **Update-2**
> 现在有了新想法：将 URL.createObjectURL 生成的视频 URL 放到 a 标签，模拟用户点击将视频数据保存到本地，再从本地加载视频播放
> 失败 Again，无法播放连续的视频 blob
>
>
> **Update-3**
> 曲线救国了家人们，之前进入了一个思维误区：数据从前端（主播）发出，经过后端服务中转，再发回前端（用户）。
> 后端服务器还没有利用起来，可以在后端将视频数据转换成 mp4/m3u8 保存到文件服务器，ws 发回前端（用户）的数据只需要发送 mp4/m3u8 文件的地址即可
> 注意：后端在进行视频格式转换的过程中需要使用到其他工具：ffmpeg
>
>
> **Update-4**
> 初期的设想是：前后端以及存储的视频全部当成二进制流来处理，不生成中间过渡文件。但是天不随人意啊家人们，Update-2/3 中出现的问题就让我断了这念头。
> 于是借助工具 ffmpeg，将生成的视频文件通过 nginx 来管理。
> 这样一来，数据传输就从原来的：前端（主播） => 后端系统 => 前端（用户）；
> 变成了：前端（主播） => 后端系统 => ffmpeg 处理视频对象 => 保存到 nginx 服务器 => 前端（用户）
> 用户只需要请求一条视频链接即可，比如 http://video-site.com/upId/liveId/filename.m3u8
>
> **Update-5**
> 家人们！我悟了！只要将 m3u8 文件和 ts 文件放在一个文件夹中，视频信息都保存在 index.m3u8 中，发送  http://video-site.com/upId/liveId/index.m3u8 请求即可。

Nginx 配置
```text
 server {
     listen       8888;
     server_name  localhost;

     location / {
         root   html;
         index  index.html index.htm;
     }

     location /video/ {
         root /Users/gnl/Tmp/statics/; # 静态资源路径
         autoindex on;

         # 允许跨域访问
         add_header Access-Control-Allow-Origin '*';
         add_header Access-Control-Allow-Credentials true;
         add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS';
         add_header Access-Control-Allow-Headers 'Range';
     }

     location /image/ {
         root /Users/gnl/Tmp/statics/; # 静态资源路径
         autoindex on;
     }
```

前端基于 vue 使用 video.js 来播放 m3u8 格式视频
```javascript
import { onMounted, ref } from 'vue';
import videojs from "video.js";

let videoSource = ref('http://localhost:8888/video/index.m3u8')

onMounted(() => {
   const player = videojs('videoJS')
   player.play()
})

```
```html
<video id="videoJS" class="video-js" autoplay muted controls preload="auto" >
  <source :src="videoSource" />
</video>
```

> **Update-6**
> 可以引入 javacv 这个依赖，就可以直接从 byte[] 二进制直接转成 m3u8 格式，不用先生成 mp4 再转 m3u8。
> 又少了一个一个中间商赚差价。
> 实际上这些关于媒体的操作应该丢到 media-server 上去操作的，现在有点懒，先放在 client 里面。
```xml
<!-- https://mvnrepository.com/artifact/org.bytedeco/javacv -->
<dependency>
   <groupId>org.bytedeco</groupId>
   <artifactId>javacv</artifactId>
   <version>latest</version>
</dependency>
```

> **Update-7**
> 思考了一下， 这 tm 不对呀！我转成 mp4 是生成中间商，生成 tmp 文件也是中间商啊！只能先观察一下这两种方法性能上有无差别。
>
> **Update-8**
> 上当了家人们 video.js 不适合播放这种持续发送过来的 m3u8 链接，尝试使用 hls.js
>
> **Update-9**
> hls.js 在这种场景下使用很合适。现在遇到的问题是：byte[] => mp4 => m3u8 消耗时间和资源都比较大，只有 websocket 发送间隔达到 5s 左右才能稍微稳定播放。
> 而且生成的 mp4 视频不一定完整。下一步试试直接 byte[] 二进制 => m3u8
>
> **Update-10**
> 尝试引入第三方依赖来将 byte[] 视频数据直接转换成 m3u8，过程有点坎坷。有两个选择
> * javacv 需要配合 ffmpeg-platform 使用，且版本号需对应
> * jcodec
>
> **Update-11**
> 被 type[] to m3u8 搞烦了

<br>

## 感想

实际上业内实现直播系统很多是通过 HLS（HTTP Live Stream） 协议来完成的，本项目没有涉及到，因为当初想不夹带太多第三方的东西，只根据前后端纯原生实现。
后续会研究如何使用 HLS 搭建直播系统

<br>

## 参考
* [从 Fetch 到 Streams](https://juejin.cn/post/6844904029244358670)

