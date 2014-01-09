package irt.flash.presentation.panel.edit_profile;

import org.junit.Before;
import org.junit.Test;

public class FrequencySetPanelTest {

	private FrequencySetPanel frequencySetPanel;

	@Before
	public void setUp() throws Exception {
		frequencySetPanel = new FrequencySetPanel();
	}

	@Test
	public void testSetFrequencySet() {
		frequencySetPanel.setValue("11GHz 23456000 6MHz");
	}

}
