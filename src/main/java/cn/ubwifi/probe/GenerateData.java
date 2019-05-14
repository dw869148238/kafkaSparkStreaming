package cn.ubwifi.probe;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.*;

public class GenerateData {
	private static final int PORT = 9999;
	private static Random random = new Random(47);
	private static final ExecutorService EXECUTORS = Executors.newSingleThreadExecutor();
	private static BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(10000);

	public static void main(String[] args) throws Exception {
		EXECUTORS.execute(new GenerateServer(blockingQueue));
        while (true) {
        	blockingQueue.put(generateStr());
			TimeUnit.MILLISECONDS.sleep(10);
		}
	}

	private static String generateStr() {
		return String.valueOf(random.nextInt(10));
	}


	private static class GenerateServer implements Runnable {

		private BlockingQueue<String> blockingQueue;

		public GenerateServer(BlockingQueue<String> blockingQueue) {
			this.blockingQueue = blockingQueue;
		}

		@Override
		public void run() {
			try (ServerSocket serverSocket = new ServerSocket(PORT);
				 Socket socket = serverSocket.accept();
				 PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
			) {
                while (true) {
                	String event = blockingQueue.take();
                	System.out.println(String.format("write string: [%s] to socket", event));
                	printWriter.write(event);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
