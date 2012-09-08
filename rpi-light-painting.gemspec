# -*- encoding: utf-8 -*-
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'rpi-light-painting/version'

Gem::Specification.new do |gem|
  gem.name          = "rpi-light-painting"
  gem.version       = RpiLP::VERSION
  gem.authors       = ["Martin Foot"]
  gem.email         = ["martin@mfoot.com"]
  gem.description   = %q{A gem for working with my light painting wand}
  gem.summary       = %q{}
  gem.homepage      = ""

  gem.files         = `git ls-files`.split($/)
  gem.executables   = gem.files.grep(%r{^bin/}).map{ |f| File.basename(f) }
  gem.test_files    = gem.files.grep(%r{^(test|spec|features)/})
  gem.require_paths = ["lib"]

  gem.add_dependency("wiringpi")
  gem.add_dependency("rmagick")
end
