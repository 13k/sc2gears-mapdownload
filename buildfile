# vim: ft=ruby
# encoding: utf-8

require 'buildr/groovy'

if groovy_artifact = Buildr.settings.build['groovy']
  artifact_ns(Buildr::Groovy::Groovyc).groovy = groovy_artifact
end

VERSION_NUMBER = "1.0.0"
GROUP = "sc2gears-plugins"
AUTHOR = {
  :first => "Kiyoshi",
  :last => "Murata",
  :email => "13k@linhareta.net"
}
COPYRIGHT = "#{AUTHOR[:first]} #{AUTHOR[:last]} <#{AUTHOR[:email]}>"

PLUGIN_MANIFEST = "Sc2gears-plugin.xml"

repositories.remote << "http://www.ibiblio.org/maven2"
repositories.remote << "http://repo1.maven.org/maven2"

Dir[File.expand_path "../buildr/*.rb", __FILE__].each do |lib|
  require lib
end

Project.local_task :deploy

desc "Map Download Sc2gears plugin"
define "Map Download" do
  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT

  plugin_manifest = filter("src/plugin") \
    .into('target') \
    .using(:erb, {
      :project => project,
      :author => AUTHOR,
      :artifacts => ARTIFACTS
    })
  
  resources.enhance { plugin_manifest.run }

  compile.using :groovyc
  compile.with ARTIFACTS[:sc2gearspluginapi].to_s

  test.using :rspec

  jar = package :jar

  task :deploy => :package do
    if sc2gears = Buildr.settings.build['sc2gears']
      raise RuntimeError, "Sc2gears path #{sc2gears.inspect} not found" unless File.exists?(sc2gears)
      plugin_path = File.join sc2gears, "Plugins", project.name
      mkpath plugin_path
      cp jar.name, plugin_path
      cp _("target/#{PLUGIN_MANIFEST}"), plugin_path
      cp artifact_ns(Buildr::Groovy::Groovyc).groovy.artifact.to_s, plugin_path
      cp artifact_ns(Buildr::Groovy::Groovyc).asm.artifact.to_s, plugin_path
    else
      puts "Sc2gears path not set in build.yml"
    end
  end
end
