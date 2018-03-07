
package irt.flash.data;

import java.util.concurrent.ThreadFactory;

public class MyThreadFactory implements ThreadFactory {

	@Override
	public Thread newThread(Runnable r) {
		return getThread(r);
	}

	public static Thread startThread(Runnable r) {
		Thread t = getThread(r);
		t.start();
		return t;
	}

	private static Thread getThread(Runnable r) {
		Thread t = new Thread(r);
		int priority = t.getPriority();
		if(priority>Thread.MIN_PRIORITY)
			t.setPriority(--priority);
		t.setDaemon(true);
		return t;
	}

}
