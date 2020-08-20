package irt.flash.helpers;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;

public class ThreadWorker {

	private final String name;
	private final Runnable runnable;

	public ThreadWorker(String name, Runnable runnable) {
		this.name = name;
		this.runnable = runnable;
	}

	public void start() {
		runThread(runnable);
	}

	@Override
	public String toString() {
		return name;
	}

	public static Thread runThread(Runnable target) {

		Thread t = new Thread(target);
		t.setDaemon(true);
		Optional.of(t.getPriority()).filter(p->p>Thread.MIN_PRIORITY).ifPresent(p->t.setPriority(--p));
		t.start();
		
		return t;
	}

	public static <T> FutureTask<T> runFutureTask(Callable<T> callable) {

		FutureTask<T> task = new FutureTask<>(callable);
		runThread(task);
		
		return task;
	}

	public static <T> FutureTask<T> runFxFutureTask(Callable<T> callable) {

		FutureTask<T> task = new FutureTask<>(callable);
		Platform.runLater(task);
		
		return task;
	}
}
