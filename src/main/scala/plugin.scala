package org.u4gvn.sc2gears.plugins.mapdownload

import scala.collection.mutable._

import java.io.File
import java.net.URI
import java.util.EnumSet

import java.awt._
import javax.swing._

import hu.belicza.andras.sc2gearspluginapi._
import hu.belicza.andras.sc2gearspluginapi.api._
import hu.belicza.andras.sc2gearspluginapi.api.listener._
import hu.belicza.andras.sc2gearspluginapi.api.sc2replay._
import hu.belicza.andras.sc2gearspluginapi.api.ui._
import hu.belicza.andras.sc2gearspluginapi.impl.BasePlugin

class MapDownloadPlugin extends BasePlugin {
	case class DownloadResult(downloader: IDownloader, success: Boolean, extraArgs: HashMap[String, Any] = null)

	case class DownloadResultCallback(callback: Boolean => Unit) extends DownloaderCallback {
		def downloadComplete = callback
	}

	class ReplayMapJob(file:File) {
		private var target:File = null
		private var url:String = null
		var downloader:IDownloader = null

		val replay = generalServices.getReplayFactoryApi
			.parseReplay(file.getAbsolutePath, EnumSet.allOf(classOf[ReplayFactoryApi.ReplayContent]))

		if (replay != null) {
			{
				val mapsFolder = generalServices.getInfoApi.getSc2MapsFolder
				target = new File(mapsFolder, replay.getMapFileName)
				url = new URI(replay.getGateway.depotServerUrl).resolve(target.getName).toString
			}
		}
		
		val valid = replay != null

		def complete() = { (target != null) && target.exists }

		def download(callback: DownloadResult => Unit, extraArgs: HashMap[String, Any]) = {
			if (downloader == null) {
				downloader = generalServices.getGeneralUtilsApi
					.getDownloader(url, target, true, DownloadResultCallback(success:Boolean => {
						callback(DownloadResult(downloader, success, extraArgs))
					})
			}
			downloader
		}
	}

	private var menuItemHandler = -1
	private var downloadIcon:ImageIcon = null

	override def init(pluginDescriptor:PluginDescriptor, pluginServices:PluginServices, generalServices:GeneralServices) = {
		super.init(pluginDescriptor, pluginServices, generalServices)

		try {
			val icons = Class.forName("hu.belicza.andras.sc2gears.ui.icons.Icons")
			downloadIcon = icons.getField("DRIVE_DOWNLOAD").get(null) match {
				case x:ImageIcon => x
				case _ => null
			}
		} catch {
			case _ => null
		}
		
		menuItemHandler = generalServices.getCallbackApi().addReplayOpsPopupMenuItem("Download missing maps", downloadIcon, new ReplayOpsPopupMenuItemListener() {
			override def actionPerformed(files:Array[File], replayOpCallback:ReplayOpCallback, handler:Integer):Unit = {
				val guiUtils = generalServices.getGuiUtilsApi
				
				val dialog = new JDialog(guiUtils.getMainFrame, generalServices.getLanguageApi.getText("mapdownloadplugin.title"))
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
				dialog.setIconImage(downloadIcon.getImage)

				val jobsQueue = new SynchronizedQueue[ReplayMapJob]()
				val workingSet = new HashSet[IDownloader] with SynchronizedSet[IDownloader]
				
				// progressbar				
				jobsQueue.enqueue(files map { new ReplayMapJob(_) })

				while (jobsQueue.count > 0) {
					if (workingSet.count < 5) {
						val job = jobsQueue.dequeue
						val download = job.download(result:DownloadResult => {
							workingSet -= result.download
						}, dialog.getComponentCount)
						dialog.add(download.getProgressBar, BorderLayout.SOUTH)
						workingSet += download
					}
				}
				
				/*
				val progressDialog = guiUtils.createProgressDialog("Map Download", downloadIcon, files.length)
				val downloads = new ArrayBuffer[IDownloader]()
				
				for (file <- files) {
					progressDialog.updateProgressBar
					
					if (progressDialog.isAborted) {
						downloads.foreach(_.requestToCancel)
						progressDialog.updateProgressBar
						progressDialog.taskFinished
						return
					}
					
					val job = new ReplayMapJob(file)
					
					if (!job.valid || job.complete) {
						progressDialog.incrementProcessed
					} else {
						val download = job.download(new DownloaderCallback() {
							def downloadFinished(success:Boolean) = {
								if (!success) { progressDialog.incrementFailed }

								progressDialog.incrementProcessed
								progressDialog.updateProgressBar

								if (progressDialog.getProcessed == progressDialog.getTotal) {
									progressDialog.updateProgressBar
									progressDialog.taskFinished
								}
							}
						})
						
						download.startDownload
						downloads += download
					}
				}
				*/
			}
		})
	}

	def destroy() = {
		generalServices.getCallbackApi.removeReplayOpsPopupMenuItem(menuItemHandler)
	}
}