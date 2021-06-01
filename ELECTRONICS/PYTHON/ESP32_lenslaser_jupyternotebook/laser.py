# -*- coding: utf-8 -*-
"""
Created on Sun Jan 17 14:05:08 2021

@author: diederichbenedict
"""
import time
import serial
import numpy as np
import fnmatch
from baseserial import espserial


class laser(espserial):
    '''
    Define laseres
    
    const char *CMD_LAS1 =    "LAS1";
    const char *CMD_LAS2 =    "LAS2";
    '''

    cmd_pre = "LAS"

    def __init__(self, serialconnection = None, laser_id = 1, pwmresolution=2^10):
        super().__init__(serialconnection=serialconnection)
        self.laser_pwm_max = pwmresolution
        self.laser_id = laser_id
        self.laserpower = 0
        self.initlaser()

    def initlaser(self):
        self.set_power(self.laserpower)
        
    def set_power(self, power=0):
        # send command()
        self.laserpower = power
        cmd = self.cmd_pre + str(self.laser_id)
        self.send(*(cmd, int(power)))
        
    def get_power(self):
        return self.laserpower
    
    def close(self):
        print("Shuttig down the laseres")
        self.set_power(0)
        

    
# testing
if __name__ == '__main__':

    # initiliaze Serial
    serialport = "/dev/ttyUSB0"
    serialport = "COM5"
    if not('serialconnection' in locals() and serialconnection.is_open):
        serialconnection = serial.Serial(serialport,115200,timeout=1) # Open grbl serial port

    print('Initializing laser 1')
    # init laser
    laser_1 = laser(serialconnection, laser_id = 1)
    las_power1 = laser_1.get_power()
    laser_1.set_power(las_power1+2000)

    print('Initializing laser 1')
    # init laser
    laser_2 = laser(serialconnection, laser_id = 2)
    las_power2 = laser_2.get_power()
    laser_2.set_power(las_power2+2000)

    # shut down laseres
    laser_1.close()
    laser_2.close()
    
    serialconnection.close()