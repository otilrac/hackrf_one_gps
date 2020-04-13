# hackrf one gps

## 说明

## 常见问题

### 异常1

问题：运行过程中出现下面错误

```
Exception in thread "main" java.net.UnknownHostException: cddis.gsfc.nasa.gov
        at java.net.Inet6AddressImpl.lookupAllHostAddr(Native Method)
        at java.net.InetAddress$2.lookupAllHostAddr(InetAddress.java:929)
        at java.net.InetAddress.getAddressesFromNameService(InetAddress.java:1324)
        at java.net.InetAddress.getAllByName0(InetAddress.java:1277)
        at java.net.InetAddress.getAllByName(InetAddress.java:1193)
        at java.net.InetAddress.getAllByName(InetAddress.java:1127)
        at java.net.InetAddress.getByName(InetAddress.java:1077)
        at org.apache.commons.net.SocketClient.connect(SocketClient.java:202)
        at cn.uvghoj.oljw.oced.Application.main(Application.kt:67)
        at cn.uvghoj.oljw.Main.main(Main.kt:8)
```

回答：这是访问国外数据文件失败，请确定当前的网络可以正常访问国外网址。