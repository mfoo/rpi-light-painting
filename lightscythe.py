#!/usr/bin/python

# A web.py application for light painting with the Adafruit Digital Adressable
# RGB LED flex strip. The web application lets users choose an image, which is
# then played column by column on the light strip.
#
# With a Raspberry Pi running a DHCP server and configured to set up an ad-hoc
# wireless network on boot, this script runs a web.py web server that serves a
# simple page that can configure and control the RGB strip via any device that
# connects to the network and browses to the URL of the server.

# Adafruit Digital Addressable RGB LED flex strip.
# ----> http://adafruit.com/products/306

import RPi.GPIO as GPIO, Image, time
from math import floor
from PIL import Image
from threading import Timer
import thread
import web

# Paths for web.py
urls = (
  '/static/.+', 'static',
  '/start', 'start',
  '/stop', 'stop',
  '/pause', 'pause',
  '/continue', 'resume',
  '/faster', 'faster',
  '/slower', 'slower',
  '/', 'index'
)

# Configurable values
filename  = "london.png"
dev       = "/dev/spidev0.0"

spidev    = file(dev, "wb")

# Setup of web.py templates
render = web.template.render('templates/')

class LightStick:
  """Represent the state of a light stick"""
  def __init__(self):
    self.paused = False
    self.updateRate = 0.01

  def faster(self, amount):
    self.updateRate += amount

  def slower(self, amount):
    if self.updateRate - amount > 0:
      self.updateRate -= amount

  def start(self):
    self.working = True
    print "Loading..."
    img       = Image.open(filename).convert("RGB")
    pixels    = img.load()
    width     = img.size[0]
    height    = img.size[1]
    print "Loaded image of %dx%d pixels" % img.size

    # Create list of bytearrays, one for each column of image.
    # R, G, B byte per pixel, plus extra '0' byte at end for latch.
    print "Allocating..."
    self.column = [0 for x in range(width + 1)]
    for x in range(width + 1):
     self.column[x] = bytearray(height * 3 + 1)

    for x in range(0, width):
      for y in range(0, height):
        self.column[x][y * 3] = 0x80 | (pixels[x,y][1] >> 2)
        self.column[x][y * 3 + 1] = 0x80 | (pixels[x,y][0] >> 2) 
        self.column[x][y * 3 + 2] = 0x80 | (pixels[x,y][2] >> 2) 

    # Set the last column to black
    for y in range(0, height):
      self.column[width][y * 3] = 0x80
      self.column[width][y * 3 + 1] = 0x80
      self.column[width][y * 3 + 2] = 0x80

    print "Rendering..."

    self.working == True
    thread.start_new_thread(self.step, ())

  def stop(self):
    self.working = False

  def step(self):
    while self.working:
      if not self.paused:
        for col in self.column:
          spidev.write(col)
#          spidev.flush()
        time.sleep(self.updateRate)
      else:
        time.sleep(0.5)

  def pause(self):
    self.paused = True

  def resume(self):
    self.paused = False

class static:
  """Serves all of the static assets in the static folder"""
  def GET(self, path):
    raise web.seeother(path)

class index:
  """Main web app entry point"""
  def GET(self):
    return render.index()

class start:
  """Start the time lapse"""
  def GET(self):
    lightstick.start()
    return "Success"

class stop:
  """Stop the time lapse"""
  def GET(self):
    lightstick.stop()
    return "Success"

class pause:
  """Pause the time lapse"""
  def GET(self):
    lightstick.pause()
    return "Success"

class resume:
  """Resume the lightstick after it was paused"""
  def GET(self):
    lightstick.resume()
    return "Success"

class faster:
  def GET(self):
    lightstick.faster(0.05)

class slower:
  def GET(self):
    lightstick.slower(0.05)

lightstick = LightStick()

if __name__ == "__main__":
  app = web.application(urls, globals())
  app.run()
