package cn.uvghoj.oljw

import cn.maizz.kotlin.extension.java.io.deleteIfExist
import cn.maizz.kotlin.extension.java.util.format
import org.apache.commons.compress.compressors.z.ZCompressorInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.log4j.PropertyConfigurator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.*

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        PropertyConfigurator.configureAndWatch("./build/config/log4j.properties")
        Application().main(args)
    }
}

class Application {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    private val toolsFile = File("./build/tools")
    private val runtimeFile = File("./build/runtime")

    @Throws
    fun main(args: Array<String>) {
        logger.info("==============================================================================")
        logger.info("== 获取GPS坐标地址，请用浏览器打开：t.cn/RJ23kUj  输入完成按回车继续")
        logger.info("== 选择位置后，复制[腾讯高德]经纬度信息，样例：25.7702524961,123.5443496704")
        logger.info("== 备注：不同手机软件内部使用的地图不同，经纬度也有所偏差，请优先使用手机软件内的经纬度")
        logger.info("------------------------------------------------------------------------------")
        logger.info("== 作者：Sollyu  地址：github.com/sollyu/hackrf_one_gps  版本：{}", Application::class.java.`package`.implementationVersion)
        logger.info("==============================================================================")
        logger.info("请输入GPS经纬度：")
        val gpsLocationPositions: List<Double> = readLine()?.trim()?.split(",")?.map { it.toDouble() } ?: throw IllegalArgumentException()
        if (gpsLocationPositions.size != 2) {
            logger.error("您输入的经纬度坐标有误，程序即将退出。")
            return
        }
        logger.debug("main|gpsLocationString={}", gpsLocationPositions)
        logger.info("请输入经纬度所在地区的海拔高度信息，默认 100米：")
        val gpsLocationHeight: Int = readLine()?.toIntOrNull() ?: 100
        logger.debug("main|gpsLocationHeight={}", gpsLocationHeight)
        logger.info("本次操作需要下载国外数据文件，是否配置代理？（不输入不配置代理信息）")
        logger.info("目前代理模式仅支持socke5代理模式，输入代理端口号：")
        val proxyInfo: Int? = readLine()?.trim()?.toIntOrNull()
        logger.debug("main|proxyInfo={}", proxyInfo)
        if (proxyInfo != null) {
            logger.debug("配置代理端口：{}", proxyInfo)
        }

        if (toolsFile.isDirectory.not())
            throw IOException("工具文件夹并不存在，请全部解压或联系相关人员")
        if (runtimeFile.exists())
            FileUtils.deleteDirectory(runtimeFile)
        if (runtimeFile.mkdirs().not())
            throw IOException("创建文件失败：" + runtimeFile.absolutePath)

        val ftpDirectory: String = String.format("/pub/gps/data/daily/%s/brdc/", Date().format(format = "Y"))
        val ftpClient: FTPClient = FTPClient()

        logger.info("开始下载国外数据文件，此操作主要看您的网络情况，请稍后~")
        if (proxyInfo != null) {
            ftpClient.proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", proxyInfo))
            logger.debug("main|ftp set proxy 127.0.0.1:{}", proxyInfo)
        }

        logger.debug("main|ftp connect")
        ftpClient.connect("cddis.gsfc.nasa.gov", 21)

        logger.debug("main|ftp login")
        if (ftpClient.login("Anonymous", "").not())
            throw RuntimeException("国外数据登录失败，请检查网络。")

        logger.debug("main|ftp change working directory {}", ftpDirectory)
        ftpClient.changeWorkingDirectory(ftpDirectory)

        ftpClient.enterLocalPassiveMode()
        logger.debug("main|ftp list files")
        val ftpLastFile: FTPFile = ftpClient
            .listFiles(null) { file: FTPFile? -> file?.name?.endsWith("n.Z") == true }
            .last()

        logger.debug("main|ftp download file {}", ftpLastFile.name)
        val localFile: File = File(runtimeFile, ftpLastFile.name)
        localFile.outputStream().let { fileOutputStream: FileOutputStream ->
            ftpClient.retrieveFile(ftpLastFile.name, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        }

        logger.debug("main|ftp logout")
        ftpClient.logout()

        // 解压文件
        localFile.inputStream().let { fileInputStream: FileInputStream ->
            val zCompressorInputStream = ZCompressorInputStream(fileInputStream)
            File(runtimeFile, localFile.nameWithoutExtension).outputStream().let { fileOutputStream: FileOutputStream ->
                fileOutputStream.write(zCompressorInputStream.readBytes())
                fileOutputStream.flush()
                fileOutputStream.close()
            }
            fileInputStream.close()
        }
        localFile.deleteIfExist()


        // 生成数据
        logger.info("制作GPS数据，约需要2分钟，请稍等~")
        val generateSimulatorCommand: Array<String> = arrayOf("cmd.exe", "/C", "start", "/WAIT", "gps-sdr-sim", "-e", localFile.nameWithoutExtension, "-l", gpsLocationPositions.joinToString(",").plus(",").plus(gpsLocationHeight), "-b 8")
        logger.debug("main|generateSimulatorCommand={}", generateSimulatorCommand)
        exec(*generateSimulatorCommand)

        // 发送信号
        logger.info("------------------------------------------------------------------------------")
        logger.info("开始发送GPS信号数据 此定位并非立即生效 手机约需要1~3分钟的反应时间。")
        logger.info("备注：为了更快速的成功定位，可参考以下建议设置：")
        logger.info("　　　1. 开启手机飞行模式，仅开启定位，定位成功后再恢复。")
        logger.info("　　　2. 关闭手机的AGPS等配置，使用仅GPS定位")
        logger.info("　　　3. 将手机尽量的靠近发射天线处")
        logger.info("　　　4. 手机应先使用地图或者GPS检查软件判断定位是否成功")
        logger.info("如若想退出发送，可直接关闭发送数据窗口。")
        val sendSingleCommand: Array<String> = arrayOf("cmd.exe", "/C", "start", "/WAIT", "hackrf_transfer", "-t", "gpssim.bin", "-f", "1575420000", "-s", "2600000", "-a", "1", "-x", "0", "-R")
        logger.debug("main|sendSingleCommand={}", sendSingleCommand)
        exec(*sendSingleCommand)

    }


    private fun exec(vararg args: String) {
        val process: ProcessBuilder = ProcessBuilder(*args)
        process.environment()["path"] = System.getenv("PATH") + ";" + toolsFile.absolutePath + "\\;"
        process.directory(runtimeFile)
        process.start().waitFor()
    }

}