
require 'date'
require File.join(File.dirname(__FILE__), 'lib/oj/version')

Gem::Specification.new do |s|
  s.name = "oj"
  s.version = ::Oj::VERSION
  s.authors = "Peter Ohler"
  s.date = Date.today.to_s
  s.email = "peter@ohler.com"
  s.homepage = "http://www.ohler.com/oj"
  s.summary = "A fast JSON parser and serializer."
  s.description = %{The fastest JSON parser and object serializer. }
  s.licenses = ['MIT']

  s.files = Dir["{lib,ext,test}/**/*.{rb,h,c}"] + ['LICENSE', 'README.md']

  if defined? JRUBY_VERSION
    s.files << "lib/oj.jar"
    s.platform = "java"
  else
    s.extensions = ["ext/oj/extconf.rb"]
  end

  s.require_paths = ["lib", "ext"]

  s.has_rdoc = true
  s.extra_rdoc_files = ['README.md']
  s.rdoc_options = ['--main', 'README.md']

  s.rubyforge_project = 'oj'

  s.add_development_dependency 'rake-compiler', '~> 0.9'
  s.add_development_dependency 'minitest', '~> 5'
  s.add_development_dependency 'rails', RUBY_VERSION <= '1.8.7' ? '~> 3' : '~> 4'
end
