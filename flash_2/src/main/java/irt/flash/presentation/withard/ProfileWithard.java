package irt.flash.presentation.withard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JToolBar;

public class ProfileWithard  extends JDialog{
	private static final long serialVersionUID = 1L;


	public ProfileWithard(Window owner) {
		super(owner, "Profile Wizard");

		setTitle("Profile Wizard");
		setResizable(false);
		setBounds(owner.getX(), owner.getY(), 400, 300);
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JToolBar toolBar = new JToolBar();
		getContentPane().add(toolBar, BorderLayout.SOUTH);
		
		JButton btnBack = new JButton("Back");
		toolBar.add(btnBack);
		
		JButton btnNext = new JButton("Next");
		toolBar.add(btnNext);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new CardLayout(0, 0));
		
		JPanel panel_1 = new UnitTypePanel();
		panel.add(panel_1, "name_6958299975766");
	}
}
