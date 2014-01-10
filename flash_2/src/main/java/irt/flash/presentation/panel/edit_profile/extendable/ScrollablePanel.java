package irt.flash.presentation.panel.edit_profile.extendable;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.concurrent.ExecutionException;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class ScrollablePanel extends JPanel implements Scrollable{
	private static final long serialVersionUID = -5575173251539578799L;

	private final Logger logger = (Logger) LogManager.getLogger();

	private SwingWorker<Dimension, Void> swingWorker;

	public ScrollablePanel() {
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return logger.exit(getSize());
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return (int)Math.round(visibleRect.getHeight());
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return (int)Math.round(visibleRect.getHeight());
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		logger.trace("getScrollableTracksViewportWidth() - true;");
		setPreferredSize();
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		logger.trace("getScrollableTracksViewportHeight() - false;");
		return false;
	}

	private void setPreferredSize() {
		logger.entry(swingWorker);

		if (swingWorker==null || swingWorker.isDone()) {
			swingWorker = new SwingWorker<Dimension, Void>() {

				@Override
				protected Dimension doInBackground() throws Exception {
					int y = 0, cy;

					for (Component c : getComponents()) {

						cy = (int) Math.round(c.getY() + c.getHeight());
						if (cy > y)
							y = cy;
					}

					Dimension dimension = null;
					int height = getHeight();
					logger.entry(height, y);

					if(height != y)
						dimension = new Dimension(getWidth(), y);

					return logger.exit(dimension);
				}

				@Override
				protected void done() {
					try {
						Dimension dimension = get();
						if (dimension!=null && !getPreferredSize().equals(dimension)) {
							logger.trace("dimension={}", dimension);
							setPreferredSize(dimension);
							revalidate();
						}
					} catch (InterruptedException | ExecutionException e) {
						logger.catching(e);
					}
				}
			};
			swingWorker.execute();
		}
		logger.exit();
	}
}
