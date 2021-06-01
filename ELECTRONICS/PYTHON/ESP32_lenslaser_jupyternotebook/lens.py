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


class lens(espserial):
    '''
    Define Lenses
    '''

    cmd_pre = "LENS"
    cmd_x = "X"
    cmd_z = "Z"


    pos_x = 0
    pos_z = 0

    offset_x = 1000
    offset_z = 1000

    pos_x_min = 0
    pos_z_min = 0

    def __init__(self, serialconnection = None, lens_id = 1, pwmresolution=2^10):
        super().__init__(serialconnection=serialconnection)
        self.pos_x_max = pwmresolution
        self.pos_z_max = pwmresolution
        self.lens_id = lens_id
        
        self.initlens()

    def initlens(self):
        self.move(self.offset_x, direction='X')
        self.move(self.offset_z, direction='Z')

    def move(self, position=0, direction='X'):
        # send command()
        if direction == 'X':
            self.pos_x = position
        elif direction == 'Z':
            self.pos_z = position
        else:
            return
        cmd = self.cmd_pre + str(self.lens_id) + direction
        self.send(*(cmd, int(position)))
        
        
    def SOFI(self, direction='X', is_sofi=False):
        "LX_SOFI"
        "LZ_SOFI"
        pass

    def get_position(self, direction):
        if direction == 'X':
            return self.pos_x
        elif direction == 'Z':
            return self.pos_z 

    def close(self):
        print("Shuttig down the lenses")
        self.move(position = 0, direction='X')
        self.move(position = 0, direction='Z')

    
# testing
if __name__ == '__main__':

    # initiliaze Serial
    serialport = "/dev/ttyUSB0"
    serialport = "COM5"
    if not('serialconnection' in locals() and serialconnection.is_open):
        serialconnection = serial.Serial(serialport,115200,timeout=1) # Open grbl serial port

    print('Initializing Lens 1')
    # init lens
    lens_1 = lens(serialconnection, lens_id = 1)
    pos_x2 = lens_1 .get_position(direction="X")
    lens_1.move(pos_x2+1000, "X")

    print('Initializing Lens 2')
    # init lens
    lens_2 = lens(serialconnection, lens_id = 2)
    pos_x2 = lens_2.get_position(direction="X")
    lens_2.move(pos_x2+1000, "X")

    # shut down lenses
    lens_1.close()
    lens_2.close()
    
    #    except Exception as e:
    #        print(e)
    
    serialconnection.close()