package org.u4gvn.sc2gears.plugins.mapdownload

import groovy.swing.SwingBuilder

import java.io.File
import java.net.URI

import java.awt.*
import javax.swing.*

import hu.belicza.andras.sc2gearspluginapi.*
import hu.belicza.andras.sc2gearspluginapi.api.*
import hu.belicza.andras.sc2gearspluginapi.api.listener.*
import hu.belicza.andras.sc2gearspluginapi.api.sc2replay.*
import hu.belicza.andras.sc2gearspluginapi.api.ui.*
import hu.belicza.andras.sc2gearspluginapi.impl.BasePlugin

private class ReplayMapJob {
  def file
  def mapsFolder
  def replayApi
  def utilsApi
  def url
  def target
  def replay

  protected ReplayMapJob() {}

  ReplayMapJob(Map m) {
    this.file = m.file
    this.mapsFolder = m.mapsFolder
    this.replayApi = m.replayApi
    this.utilsApi = m.utilsApi

    replay = replayApi.parseReplay file.absolutePath, EnumSet.of(ReplayFactoryApi.ReplayContent.GAME_EVENTS)

    if (replay != null) {
      target = new File(mapsFolder, replay.mapFileName)
      url = new URI(replay.gateway.depotServerUrl).resolve(target.name)
    }
  }

  def valid() { replay != null }
  def invalid() { replay == null }
  def complete() { (target != null) && target.exists() }
  def incomplete() { !complete() }

  def download() {
    if (invalid()) {
      false
    } else if (complete()) {
      true
    } else {
      target.parentFile?.mkdirs()
      utilsApi.downloadUrl(url.toString(), target)
    }
  }
}

public class MapDownloadPlugin extends BasePlugin {
  private menuItemHandler
  private downloadIcon
  private downloadDialog

  void init(PluginDescriptor pluginDescriptor, PluginServices pluginServices, GeneralServices generalServices) {
    super.init(pluginDescriptor, pluginServices, generalServices)
    try {
      def icons = Class.forName("hu.belicza.andras.sc2gears.ui.icons.Icons")
      downloadIcon = (ImageIcon) icons.getField("DRIVE_DOWNLOAD").get(null)
    } catch(e) {}

    menuItemHandler = generalServices
      .callbackApi
      .addReplayOpsPopupMenuItem("Download missing maps", downloadIcon, menuItemListener())
  }

  void destroy() {
    generalServices.callbackApi
      .removeReplayOpsPopupMenuItem(menuItemHandler)
  }

  def menuItemListener() {
    new ReplayOpsPopupMenuItemListener() {
      void actionPerformed(File[] files, ReplayOpCallback replayOpCallback,
          Integer handler)
      {
        createDownloadDialog {
          def progress
          def status
          def downloadListModel
          def downloadList

          def setStatus = { String msg -> status.text = msg }
          def currentStatus = { String name -> progress.string = name }
          def incrementProgress = { progress.value = progress.value + 1 }

          hbox {
            status = label()
            progress = progressBar(
              indeterminate: false,
              minimum: 0,
              maximum: files.length,
              value: 0,
              stringPainted: true)
          }

          scrollPane {
            downloadListModel = new DefaultListModel()
            downloadList = list(model: downloadListModel, layoutOrientation: JList.VERTICAL)
          }

          doOutside {
            edt { setStatus "Parsing" }

            def api = generalServices.replayFactoryApi
            def utils = generalServices.generalUtilsApi
            def folder = generalServices.sc2MapsFolder

            def jobs = files.collect { f ->
              try {
                edt { currentStatus f.name }
                def replay = new ReplayMapJob(file: f, mapsFolder: folder, replayApi: api, utilsApi: utils)
                edt { incrementProgress() }
                replay
              } catch(e) {
                println "shit happened: ${e}\n${e.stackTrace.join("\n")}"
                throw e
                null
              }
            }.findAll { j ->
              try {
                j?.incomplete()
              } catch (e) {
                println "shit happened: ${e}\n${e.stackTrace.join("\n")}"
                throw e
              }
            }

            edt {
              try {
                progress.value = 0
                progress.maximum = jobs.size
                currentStatus ""
                setStatus "Downloading"
              } catch (e) {
                println "shit happened: ${e}\n${e.stackTrace.join("\n")}"
                throw e
              }
            }

            jobs.each { job ->
              try {
                def currentDownload = "${job.replay.mapName} (${job.replay.gateway})"
                edt { currentStatus currentDownload }
                job.download()
                edt {
                  incrementProgress()
                  downloadListModel.addElement currentDownload
                }
              } catch (e) {
                println "shit happened: ${e}\n${e.stackTrace.join("\n")}"
                throw e
              }
            }

            doLater {
              try {
              setStatus "Finished"
              progress.visible = false
              } catch (e) {
                println "shit happened: ${e}\n${e.stackTrace.join("\n")}"
                throw e
              }
            }
          }
        }
      }
    }
  }

  def createDownloadDialog(Closure content) {
    new SwingBuilder().edt
    {
      def guiUtils = generalServices.guiUtilsApi

      frame(guiUtils.mainFrame) {
        downloadDialog = dialog(title: "Map Downloada", size: [768, 250],
            defaultCloseOperation: JDialog.DISPOSE_ON_CLOSE,
            iconImage: downloadIcon?.image, show: true)
        {
          vbox {
            vbox(content)

            hbox {
              button(text: "Close", actionPerformed: { downloadDialog.dispose() })
            }
          }
        }
      }

      guiUtils.centerWindowToWindow(downloadDialog, guiUtils.mainFrame);
    }
  }
}
