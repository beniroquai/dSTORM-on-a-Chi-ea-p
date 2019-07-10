# -*- coding: utf-8 -*-
import skvideo.io
import skvideo.datasets
import numpy as np
import tifffile as tif
import matplotlib.pyplot as plt
import NanoImagingPack as nip 


#myvideofile = = 'C://Users//diederichbenedict//Dropbox//Camera Uploads//'
#myvideofile = '2019-06-13_15.07.46_Fluctuation_Cardio_Mayoblast_Cellphone_Largelens.mp4'

myvideopath = 'C://Users//diederichbenedict//Dropbox//STORMonAcheaip//STORM-on-a-chea(i)p//'
myvideofile = 'MOV_2019_07_09_17_24_52.mp4'

#myvideopath = '/Users/bene/Downloads/'
#myvideofile = '2019-06-20 18.41.00_blink.mp4'

outputfile = myvideofile+'.tif'
myvideofile = myvideopath+myvideofile
videogen = skvideo.io.vreader(myvideofile)

myroisize = 1024
iiter = 0
myimlist = []
for frame in videogen:
    gray = np.mean(frame, 2)
    gray = nip.extract(gray, myroisize)
    #nip.view(gray)
    #tif.imsave(outputfile, np.uint8(gray), append=True, bigtiff=True) #compression='lzw',     
    iiter+=1
    print(iiter)
    myimlist.append(gray)
    
myimage = np.array(myimlist)
tif.imsave(outputfile, np.uint8(myimage), append=False, bigtiff=True) #compression='lzw',     