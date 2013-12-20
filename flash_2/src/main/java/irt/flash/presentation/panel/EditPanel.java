package irt.flash.presentation.panel;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class EditPanel extends JPanel {

	private static final long serialVersionUID = -5075735149795277563L;

	public EditPanel(String title) {
		{
			setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0), 1, true), title, TitledBorder.LEFT, TitledBorder.TOP, getFont().deriveFont(16F), Color.BLUE));
			addAncestorListener(new AncestorListener() {
				public void ancestorMoved(AncestorEvent event) { }
				public void ancestorRemoved(AncestorEvent event) { }
				public void ancestorAdded(AncestorEvent event) {
					setBackground(getParent().getBackground().brighter());
				}
			});
		}
	}
}
