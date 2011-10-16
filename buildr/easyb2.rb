require 'buildr/groovy'

module Buildr::Groovy
  # EasyB is a Groovy based BDD framework.
  # To use in your project:
  #
  #   test.using :easyb2
  #
  # This framework will search in your project for:
  #   src/spec/groovy/**/*.groovy
  #
  # Support the following options:
  # * :format -- Report format :txt or :xml, default is :txt
  # * :properties -- Hash of properties passed to the test suite.
  # * :java_args -- Arguments passed to the JVM.
  class EasyB2 < TestFramework::JavaBDD
    @lang = :groovy
    @bdd_dir = :spec

    VERSION = "0.9.8"
    OPTIONS = [:format, :properties, :java_args]
    TESTS_GLOB = "**/*.groovy"

    REQUIRES = ArtifactNamespace.for(self) do |ns|
      ns.easyb = "org.easyb:easyb:jar:#{VERSION}"
    end

    class << self
      def dependencies
        @dependencies ||= REQUIRES.artifacts
      end

      def applies_to?(project) #:nodoc:
        !tests(project).empty?
      end

      def tests(project)
        @test ||= Dir[project.path_to(:source, bdd_dir, lang, TESTS_GLOB)]
      end
    end

    def tests(dependencies) #:nodoc:
      self.class.tests(task.project)
    end

    def run(tests, dependencies) #:nodoc:
      options = { :format => :txt }.merge(self.options).only(*OPTIONS)

      easyb_format, ext = \
        case options[:format]
        when :txt
          ['txtstory', '.txt']
        when :xml
          ['xmlbehavior', '.xml']
        else
          raise "Invalid format #{options[:format]} expected one of [:txt, :xml]"
        end

      cmd_args = [ 'org.disco.easyb.BehaviorRunner' ]
      cmd_options = { :properties => options[:properties],
                      :java_args => options[:java_args],
                      :classpath => dependencies }

      tests.inject([]) do |passed, test|
        name = test.sub(/.*?groovy[\/\\]/, '').pathmap('%X')
        report = File.join(task.report_to.to_s, name + ext)
        mkpath report.pathmap('%d')
        begin
          Java::Commands.java cmd_args,
             "-#{easyb_format}", report,
             test, cmd_options.merge(:name => name)
        rescue => e
          passed
        else
          passed << test
        end
      end
    end
  end # EasyB2
end

Buildr::TestFramework << Buildr::Groovy::EasyB2
