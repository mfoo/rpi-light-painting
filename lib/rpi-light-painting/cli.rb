require "rpi-light-painting"
require "RMagick"
require "wiringpi"

class RpiLP::CLI

  def start
#    img = Magick::Image::read("London2011.jpeg")[0]
#    img = img.scale0.5)
##    img.write("another_filename.jpg")
#    WiringPi.mode(10,OUTPUT)
#    WiringPi.write(10,HIGH)

#    WiringPi.read(10)
    io = WiringPi::GPIO.new(WPI_MODE_SYS)
    io.write(10, HIGH)
    sleep 1
    io.write(10,LOW)
    sleep 1
    io.write(10,HIGH)

    a = []
    val = 0x00
    for i in 0..255 do
      val = val + 1
      io.shiftOut(10,11,LSBFIRST,val)
      sleep(1/255)
    #  a << [val,val,val]
    end
    #io.shiftOutArray(10, 11, 17, a)
  end
end
