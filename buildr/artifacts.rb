# encoding: utf-8

class ArtifactSpec < Struct.new(:group, :id, :type, :version, :url, :path)
  def to_s
    # JRuby 1.6.4 will throw an error when trying to use Struct#values_at
    # values_at(:group, :id, :type, :version).join(':')
    [group, id, type, version].join(':')
  end
end

ARTIFACTS = {
  :sc2gearspluginapi => ArtifactSpec.new(
    'hu.belicza.andras',
    'sc2gearspluginapi',
    'jar',
    '2.5',
    'http://www.mediafire.com/file/irips2thdudz955/Sc2gears_Plugin_API_2.5.zip',
    'Sc2gears Plugin API 2.5/lib/Sc2gears-plugin-api-2.5.jar'
  )
}

ARTIFACTS.each do |id, spec|
  zip_file = download "artifacts/packages/#{id}-#{spec.version}.zip" => spec.url
  extract = unzip("artifacts/extracted" => zip_file).include(spec.path)
  jar = file "artifacts/extracted/#{spec.path}" => extract
  artifact(spec.to_s).from jar
end
