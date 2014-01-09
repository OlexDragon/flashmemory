package irt.flash.presentation.panel.edit_profile.extendable;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public class ScrollablePanel extends JPanel implements Scrollable{
	private static final long serialVersionUID = -5575173251539578799L;

	private static final Logger logger = (Logger) LogManager.getLogger();

	public ScrollablePanel() {
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		logger.entry(visibleRect, orientation, direction);
		return logger.exit((int)Math.round(visibleRect.getHeight()));
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		logger.entry(visibleRect, orientation, direction);
		return logger.exit((int)Math.round(visibleRect.getHeight()));
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	@Override
	public void setSize(int width, int height) {

		Component[] components = getComponents();
		Component component = components[components.length-1];
		setPreferredSize(new Dimension(width, (int)Math.round(component.getLocation().getY()+component.getSize().getHeight())));
		super.setSize(width, height);
		revalidate();
	}
}
