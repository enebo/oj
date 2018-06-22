#!/usr/bin/env rake
require 'bundler/gem_tasks'
require 'rake/extensiontask'
require 'rake/testtask'

if defined? JRUBY_VERSION
  require 'rake/javaextensiontask'
  Rake::JavaExtensionTask.new('oj') do |ext|
    ext.ext_dir = 'ext/java'
    ext.source_version = '1.7'
    ext.target_version = '1.7'
  end
else
  Rake::ExtensionTask.new('oj') do |ext|
    ext.lib_dir = 'lib/oj'
  end
end

Rake::TestTask.new(:test) do |test|
  test.libs << 'test'
  test.pattern = 'test/test_*.rb'
  test.options = "-v"
end

task :test_all => [:compile] do
  exitcode = 0

  Dir.glob(File.join('test', 'isolated', 'test_*.rb')).each do |isolated|
    rout, wout = IO.pipe
    puts "\n" + "-"*10 + " File: #{isolated} " + "-"*10
    status = system("ruby -Itest #{isolated}")
    exitcode = 1 unless status
  end

  Rake::Task['test'].invoke
  exit(1) if exitcode == 1
end

task :default => :test_all


