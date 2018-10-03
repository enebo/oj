#!/usr/bin/env rake

require 'bundler/gem_tasks'
require 'rake/extensiontask'
require 'rake/testtask'

if defined? JRUBY_VERSION
  # Note: This is a little hokey but jruby developers usually have jruby
  # and MRI ruby in same shell to do quick compares.
  RUBY_EXE = 'jruby'
  require 'rake/javaextensiontask'
  Rake::JavaExtensionTask.new('oj') do |ext|
    ext.ext_dir = 'ext/java'
    ext.source_version = '1.7'
    ext.target_version = '1.7'
  end
else
  RUBY_EXE = 'ruby'
  Rake::ExtensionTask.new('oj') do |ext|
    ext.lib_dir = 'lib/oj'
  end
end

=begin
Rake::TestTask.new(:test) do |test|
  test.libs << 'test'
  test.pattern = 'test/test_*.rb'
  test.options = "-v"
end
=end

task :test_all => [:clean, :compile] do
  exitcode = 0
  status = 0

  cmds = "bundle exec #{RUBY_EXE} test/tests.rb && ruby test/tests_mimic.rb && ruby test/tests_mimic_addition.rb"
  puts "\n" + "#"*90
  puts cmds
  Bundler.with_clean_env do
    status = system(cmds)
  end
  exitcode = 1 unless status

  # Verifying that json gem tests work for native implemntation for Ruby 2.4.0
  # and above only. We know the older versions do not pass the 2.4.0 unit
  # tests.
  if RUBY_VERSION >= '2.4'
    Dir.glob('test/json_gem/*_test.rb').each do |file|
      cmd = "REAL_JSON_GEM=1 bundle exec ruby -Itest #{file}"
      puts "\n" + "#"*90
      puts cmd
      Bundler.with_clean_env do
        status = system(cmd)
      end
      exitcode = 1 unless status
    end
  end

  Rake::Task['test'].invoke
  exit(1) if exitcode == 1
end

task :default => :test_all

begin
  require "rails/version"

  if Rails.version =~ /4\.\d/
    Rake::TestTask.new "activesupport4" do |t|
      t.libs << 'test'
      t.pattern = 'test/activesupport4/*_test.rb'
      t.warning = true
      t.verbose = true
    end
    Rake::Task[:test_all].enhance ["activesupport4"]
  end

  if Rails.version =~ /5\.\d/
    Rake::TestTask.new "activesupport5" do |t|
      t.libs << 'test'
      t.pattern = 'test/activesupport5/*_test.rb'
      t.warning = true
      t.verbose = true
    end
    Rake::Task[:test_all].enhance ["activesupport5"]
  end

  Rake::TestTask.new "activerecord" do |t|
    t.libs << 'test'
    t.pattern = 'test/activerecord/*_test.rb'
    t.warning = true
    t.verbose = true
  end
  Rake::Task[:test_all].enhance ["activerecord"]

rescue LoadError
end

