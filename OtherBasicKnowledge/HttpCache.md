参考资料：
[《OKHttp源码解析(六)--中阶之缓存基础》](https://www.jianshu.com/p/b32d13655be7)

[《HTTP缓存机制详解》](https://segmentfault.com/a/1190000010775131)

_《HTTP权威指南》_

缓存的优点：

1. 减少冗余的数据传输。
2. 更快的加载页面/数据。
3. 服务器可以更快地响应。
4. 减少延迟。



如果没有缓存的世界：

>
>往大了来说，千千万个用户同时请求同一份数据，那么服务器会发送千千万次重复数据。这个数据
>又会在网络中一遍一遍传输。完全可以保留第一条服务器响应的副本，后续请求由缓存副本来对
>应。
>往小了来说，对于移动端用户，如果重复请求同一个接口，并且这个接口返回的数据是固定的。也
>会造成流量上的浪费。


缓存分为公有缓存Shared cache和私有缓存Local cache

![1](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_1.png)


如图，公有缓存和私有缓存分别储存在代理服务器(这块存疑)/客户端



我们先来思考一些问题:
如果数据发生改变，那么缓存就会过期。客户端的老数据和服务器就不同步了,应当怎么办？缓存如果更新，应当如何更新缓存？
服务器应当如何告知客户端数据改变需要更新缓存了？


这里先明确两个概念，缓存命中，和新鲜度检测(什么破翻译，跟我腊鸡英格力士有的一拼)。很好理解：

缓存情景之一

![2](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_2.png)


缓存命中就是我想要的数据有缓存，新鲜度检测就是看下有没有过期。



按照<是否向服务器发起请求，进行对比>分类，可分为强制缓存(不会每次都向服务器发送请求，直接从缓存中读取资源)和对比缓存(每次都向服务器发送请求，服务器会根据这个请求的request header的一些参数来判断是否命中协商缓存，如果命中，则返回304状态码并带上新的response header通知浏览器从缓存中读取资源)。


![3](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_3.png)
![4](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_4.png)


//其实这4个图看完就大概明白怎么回事了，接下来我们还是详细说说header里的字段和响应规则。

我们先来看强制缓存：

# 强制缓存

对于强制缓存来说，原始服务器向每个文档附加了一个“过期日期， 即响应header中会有两个字段来标明失效规则（Expires/Cache-Control）


![5](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_5.png)

Expires [ek- ɪk'spaɪɚz]是Http1.0时期的古董玩意，发送一个GMT(格林尼治时间) *Mon, 22 Jul 2002 11:12:01 GMT* 来告诉客户端过期时间，过了这个时间，就过期啦。

到了HTTP 1.1时代，新增了Cache-Control 来定义缓存过期时间。注意：若报文中同时出现了 Expires 和 Cache-Control，则以 Cache-Control 为准。

Cache-Control 主要有以下字段：

| header | 说明
| ----------- | ------------------- |
| private:       |    客户端可以缓存 |
| public:         |     客户端和代理服务器都可缓存 |
| max-age=xxx:  | “max-age 值定义了文档的最大使用期——从第一次生成文档到文档不再新鲜、无法使用为止，最大的合法生存时间（以秒为单位）”-《HTTP权威指南》 |
| no-cache: | “标识为 no-cache 的响应实际上是可以存储在本地缓存区中的。只是在与原始服务器进行新鲜度再验证之前，缓存不能将其提供给客户端使用。这个首部使用 do-not-serve-from-cache-without-revalidation 这个名字会更恰当一些。”-《HTTP权威指南》 |
| no-store: |“标识为 no-store 的响应会禁止缓存对响应进行复制。缓存通常会像非缓存代理服务器一样，向客户端转发一条 no-store 响应，然后删除对象。”-《HTTP权威指南》 |

比如：


![6](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_6.png)

我们可以看到max-age = 2592000,也就是说，在2592000秒内再次请求这条数据，都会直接获取缓存数据库中的数据，直接使用：

![7](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_7.png)

看到from disk cache了么，这就是本地缓存，并没有请求服务器。

tips:

1. 除了max-age外，我还注意到有个 s-maxage，max-age用于普通缓存，而s-maxage用于代理缓存。比如我们虽然让客户端缓存一年，但希望让代理服务器缓存一天就够了。于是在设置 max-age=31536000 的同时还可以设置 s-maxage=86400

强制缓存有个局限性就是我这里有一份文件，我也说不清楚什么时候更新，反正就是会更新，总不能让客户端时不时地来问问，“嘿，更新了没？”针对于这种情况，就是对比缓存了：

------


# 对比缓存
对比缓存，顾名思义，需要进行比较判断是否可以使用缓存。浏览器第一次请求数据时，服务器会将缓存标识与数据一起返回给客户端，它在请求header和响应header间进行传递，客户端将二者备份至缓存数据库中。

#### If-Modified-Since:	
如果从指定日期之后文档被修改过了，就执行请求的方法。可以与Last-Modified 服务器响应首部配合使用，只有在内容被修改后与已缓存版本有所不同的时候才去获取内容。

#### If-None-Match: 
服务器可以为文档提供特殊的标签（参见ETag），而不是将其与最近修改日期相匹配，这些标签就像序列号一样。如果已缓存标签与服务器文档中的标签有所不同，If-None-Match  首部就会执行所请求的方法


先来看If-Modified-Since&&Last-Modified：

![8](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_8.png)


Last-Modified：服务器在响应请求时，告诉浏览器资源的最后修改时间。

![9](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_9.png)


If-Modified-Since:再次请求服务器时，通过此字段通知服务器：上次请求时，服务器返回最远的最后修改时间。服务器收到请求后发现有If-Modified-Since则与被请求资源的最后修改时间进行对比。若资源的最后修改时间大于If-Modified-Since，说明资源又被改动过，则响应整个内容，返回状态码是200。如果资源的最后修改时间小于或者等于If-Modified-Since，说明资源没有修改，则响应状态码为304


![10](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_10.png)


 If-None-Match:
再次请求服务器时，通过此字段通知服务器客户端缓存数据的唯一标识。服务器收到请求后发现有头部If-None-Match则与被请求的资源的唯一标识(ETag)进行对比(比如这里是W/"29b3-1632b0835df")，不同则说明资源被改过，则响应整个内容，返回状态码是200。相同则说明资源没有被改动过，则响应状态码304，告知客户端可以使用缓存

![11](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_11.png)

 If-None-Match优先级要高于Last-Modified。这里发现taobao用的是： If-None-Match，知乎用的是If-Modified-Since


最后两张总结图：

![12](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_12.png)
![13](https://github.com/DingoDemon/AndroidNotes/blob/master/LinkPics/http_cache/http_cache_13.png)

