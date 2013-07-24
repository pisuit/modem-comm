import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.google.code.jgntp.Gntp;
import com.google.code.jgntp.GntpApplicationInfo;
import com.google.code.jgntp.GntpClient;
import com.google.code.jgntp.GntpNotificationInfo;
import com.google.common.base.Stopwatch;
import com.sun.imageio.plugins.common.ImageUtil;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class Listener {
	static SerialPort serialPort = new SerialPort("COM5");
	static String ip = "172.16.72.137";
	static DateTime lastTime = new DateTime();
	static DateTime lastCall = new DateTime();
	static GntpApplicationInfo info = Gntp.appInfo("Phone").build();
	static GntpNotificationInfo noinfo = Gntp.notificationInfo(info,
			"Call Receive").build();
	static GntpClient client = Gntp.client(info).forHost(ip).secure("123")
			.withoutRetry().build();

	public static void main(String[] args) {
		try {
			client.register();
			serialPort.openPort();
			serialPort.setParams(9600, 8, 1, 0);
//			int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS
//					+ SerialPort.MASK_DSR;// Prepare mask
//			serialPort.setEventsMask(mask);// Set mask
			serialPort.addEventListener(new SerialPortReader(), SerialPort.MASK_RXCHAR
                    | SerialPort.MASK_RXFLAG
                    | SerialPort.MASK_CTS
                    | SerialPort.MASK_DSR
                    | SerialPort.MASK_RLSD);

			System.out.println("Listening....");
		} catch (SerialPortException ex) {
			System.out.println(ex);
		}
	}

	static class SerialPortReader implements SerialPortEventListener {
		public void serialEvent(SerialPortEvent event) {
						if (event.isRXCHAR()) {// If data is available
				if (Seconds.secondsBetween(lastCall, DateTime.now())
						.getSeconds() > 10) {
					System.out.println("Call received on "
							+ DateTime.now().toDate().toString());
					if (isClientAvailable(ip)) {
						try {
							client.notify(Gntp
									.notification(noinfo, "Call Received !!")
									.text(DateTime.now().toDate().toString())
									.icon(getImage()).build());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					lastCall = new DateTime();
					lastTime = new DateTime();
				}
				if (Seconds.secondsBetween(lastTime, DateTime.now())
						.getSeconds() < 5
						&& Seconds.secondsBetween(lastTime, DateTime.now())
								.getSeconds() > 1) {
					lastTime = new DateTime();
					lastCall = new DateTime();
				}
			}
		}
	}

	private static boolean isClientAvailable(String ip) {
		try {
			Socket sock = new Socket();
			sock.connect(new InetSocketAddress(ip, 23053), 500);
			sock.close();
			return true;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	private static RenderedImage getImage() throws IOException {
		URL path = ImageUtil.class.getResource("/Phone-icon.png");
		return ImageIO.read(path);
	}
}
