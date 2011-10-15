package org.u4gvn.sc2gears.plugins.mapdownload

import groovy.swing.SwingBuilder

import java.awt.*
import javax.swing.*

import hu.belicza.andras.sc2gearspluginapi.*
import hu.belicza.andras.sc2gearspluginapi.api.*
import hu.belicza.andras.sc2gearspluginapi.api.listener.*
import hu.belicza.andras.sc2gearspluginapi.api.sc2replay.*
import hu.belicza.andras.sc2gearspluginapi.api.ui.*
import hu.belicza.andras.sc2gearspluginapi.impl.BasePlugin

public class MapDownloadPlugin extends BasePlugin {
	private menuItemHandler
	private downloadIcon

	void init(PluginDescriptor pluginDescriptor, PluginServices pluginServices, GeneralServices generalServices) {
		super.init(pluginDescriptor, pluginServices, generalServices)
		try {
			def icons = Class.forName("hu.belicza.andras.sc2gears.ui.icons.Icons")
			downloadIcon = (ImageIcon) icons.getField("DRIVE_DOWNLOAD").get(null)
		} catch(e) {}

		menuItemHandler = generalServices.getCallbackApi()
			.addReplayOpsPopupMenuItem("Download missing maps", downloadIcon,
				new ReplayOpsPopupMenuItemListener() {
					void actionPerformed(File[] files, ReplayOpCallback replayOpCallback, Integer handler) {
						def parent = generalServices.getGuiUtilsApi().getMainFrame()
						def swing = new SwingBuilder()
						swing
					}
				})
	}

	void destroy() {
		generalServices.getCallbackApi()
			.removeReplayOpsPopupMenuItem(menuItemHandler)
	}
}
