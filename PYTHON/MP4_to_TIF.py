# -*- coding: utf-8 -*-
import skvideo.io
import skvideo.datasets
import numpy as np
import tifffile as tif
import matplotlib.pyplot as plt 


myvideopath = 'C://Users//diederichbenedict//Dropbox//Camera Uploads//'
myvideofile = '2019-06-13_15.07.46_Fluctuation_Cardio_Mayoblast_Cellphone_Largelens.mp4'
outputfile = myvideofile+'.tif'
myvideofile = myvideopath+myvideofile
videogen = skvideo.io.vreader(myvideofile)

iiter = 0
myimlist = []
for frame in videogen:
    gray = np.mean(frame, 2)
    #tif.imsave(outputfile, np.uint8(gray), append=True, bigtiff=True) #compression='lzw',     
    iiter+=1
    print(iiter)
    myimlist.append(gray)
    
myimage = np.array(myimlist)
    
