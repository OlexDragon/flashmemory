package irt.flash.helpers;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ThreadWorker {

	public static Thread runThread(Runnable target) {

		Thread t = new Thread(target);
		Optional.of(t.getPriority()).filter(p->p>Thread.MIN_PRIORITY).map(p->--p).ifPresent(p->t.setPriority(p));
		t.start();
		
		return t;
	}

	public static <T> FutureTask<T> runFutureTask(Callable<T> callable) {

		FutureTask<T> ft = new FutureTask<>(callable);
		Thread t = new Thread(ft);
		Optional.of(t.getPriority()).filter(p->p>Thread.MIN_PRIORITY).map(p->--p).ifPresent(p->t.setPriority(p));
		t.start();
		
		return ft;
	}
}
