# CAD Files of the cellSTORM II

## CAD Design 

The microscope has four major parts (red, green, yellow, blue) as seen in the figure below. Everything is designed in Autodesk Inventor 2019 Student Edition. 
All ```.ipt``` and ```.iam``` files can be found in the ```WORKSPACE``` folder. 

All ```Iventor``` source files can be found [here](./Nanoscopy-on-the-cheap/Workspace/).

#### Rendering 

<p align="center">
<img src="./IMAGES/cellSTORM_v5.png" width="700">
</p>

#### Printed Version

<p align="center">
<img src="./IMAGES/cellSTORM_v5.jpeg" width="700">
</p>


## 🛠 3D RELEASE files for designing and printing 

All release files for the microscope as shown in the figure above can be found as [design files for Autodesk Inventor 2019](./Nanoscopy-on-the-cheap/Workspace/RELEASE/Autodesk_Inventor_2019) and as 3D printable [STL files](./Nanoscopy-on-the-cheap/Workspace/RELEASE/STL). As a 3D printer, we used Prusa 3 MK2 at a layerheight of 0.2mm and 100% infill with PLA (Prusament, space black) and ABS for the base. 

## Preparation: Electronics + Wiring 

For all electronics-related tasks we refer to [this document](https://github.com/beniroquai/dSTORM-on-a-Chi-ea-p/tree/master/ELECTRONICS#lens-wiring).

## Assembly

We give a detailed pictured tutorial to build you own **cellSTORM** device. You will require some very basic tools such as common hex-keys and some very basic skills to put everything together. It won't take more than 1h. 

### STEP 1. Build the coupling arm

Fist we want to build the fiber coupling mechanism based on the optical pick up unit (OPU) with sub-micron and a single-mode diode laser. 

##### Partslist 
|  Type | Details  |  Price | Link  |
|---|---|---|---|
| Laser |  3450 300mW 637nm Dot Laser Module TTL/analog 12VDC |  50 € | [Laserlands](https://www.laserlands.net/diode-laser-module/600nm-640nm-orange-red-laser-module/200mw-300mw-637nm-638nm-laser-diode-module-ttl-stage-lighting-dj-show-12vdc.htmll)  |
| Optical Pickup | Objektiv Optik Laser KES-400A PLAYSTATION 3 Nicht Funktioniert für Ersatzteil | 1 € | [ebay](https://www.ebay.de/itm/Objektiv-Optik-Laser-KES-400A-PLAYSTATION-3-Nicht-Funktioniert-fur-Ersatzteil/333375976216?hash=item4d9ec1b318:g:W~gAAOSwJytdEPeV)  |
| Screws | 4 * M3x20, some small Philips screws | 1 € | [shelf]()  |
| Laser Mount | 3D Print: Mechanical part to connect the Laser+OPU to the XY-stage | 5 € | [STL](./STL/cellSTORM_v5.dwg_cellSTORM_v5_laserstage.stl)  |



##### STEP 1: Get the parts ready

<p align="center">
<img src="./IMAGES/Assembly_0.jpg" width="300">
</p> 

##### STEP 2: Mount the Laser to the part with M3 screws

<p align="center">
<img src="./IMAGES/Assembly_1.jpg" width="300">
</p> 

##### STEP 3: Mount the OPU to the part with some small screws

<p align="center">
<img src="./IMAGES/Assembly_3.jpg" width="300">
</p> 

##### STEP 4: Finishing the assembly of the Laser Coupling mechanism

<p align="center">
<img src="./IMAGES/Assembly_5.jpg" width="300">
</p> 



-----------------------

### STEP 2. Assemble the XY stages

The two different XY stages for coarse coupling and selecting the field-of-view need to be mounted above/below the laser+opu part from STEP 1. It's a bit tricky and eventually need some time. 

##### Partslist 
|  Type | Details  |  Price | Link  |
|---|---|---|---|
| 2x XY-Stages  | XY Axis Manual Trimming Platform Linear Stage Tuning Sliding Table 40/50/60/90mm, 60x60mm |  80 € | [Ebay](https://www.ebay.de/itm/XY-Axis-Manual-Trimming-Platform-Linear-Stage-Tuning-Sliding-Table-40-50-60-90mm/202315259419?var=502290711250)  |
| Optical Pickup | Objektiv Optik Laser KES-400A PLAYSTATION 3 Nicht Funktioniert für Ersatzteil | 1 € | [ebay](https://www.ebay.de/itm/Objektiv-Optik-Laser-KES-400A-PLAYSTATION-3-Nicht-Funktioniert-fur-Ersatzteil/333375976216?hash=item4d9ec1b318:g:W~gAAOSwJytdEPeV)  |
| Screws | 4 * M3x20,  4 * M4x20, 4 x M3 nuts,  some small Philips screws | 1 € | [shelf]()  |
| Base Plate | 3D Print: Plate which holds the sample and the XY-stages | 5 € | [STL](./STL/cellSTORM_v5.dwg_cellSTORM_Base_v0_R1.stl)  |



##### STEP 1: Get the parts ready

<p align="center">
<img src="./IMAGES/Assembly_9.jpg" width="300">
</p> 

##### STEP 2: Connect the 2 XY-stages with the Laser-OPU part assembled in the previous part ("sandwiched")

<p align="center">
<img src="./IMAGES/Assembly_10.jpg" width="300">
</p> 

##### STEP 3: Fix the M3 screw with an M3 nut

<p align="center">
<img src="./IMAGES/Assembly_11.jpg" width="300">
</p> 

##### STEP 4: Perform step 3 on all 4 corners 

<p align="center">
<img src="./IMAGES/Assembly_12.jpg" width="300">
</p> 

##### STEP 5: Mount the "sandwiched" XY-stages to the base-plate with M4 screws

<p align="center">
<img src="./IMAGES/Assembly_14.jpg" width="300">
</p> 

##### STEP 6: Flip the assembly

<p align="center">
<img src="./IMAGES/Assembly_15.jpg" width="300">
</p> 

##### STEP 7: Add M3 screws for the sample try

<p align="center">
<img src="./IMAGES/Assembly_17.jpg" width="300">
</p> 


-----------------------

### STEP 3. Prepare the sample tray

The magnetic-snap sample tray can hold fibers, samples and photonic waveguide chips. It's snap-fit into the 3 M3 screws using ball-magnets.

##### Partslist 
|  Type | Details  |  Price | Link  |
|---|---|---|---|
| Ball Magents | T::A Kugelmagnete 6 mm N45 Neodym Magnete NdFeB Menge wählbar extrem stark | 4 € | [ebay](https://www.ebay.de/itm/T-A-Kugelmagnete-5-6-10-mm-N45-Neodym-Magnete-NdFeB-Menge-w%C3%A4hlbar-extrem-stark/122187457941?var=422432026780)  |
| Sample Tray | 3D Print: Sample Tray | 1 € | [STL](./STL/cellSTORM_v5_fibertray_0.stl)  |



##### STEP 1: Get the parts ready

<p align="center">
<img src="./IMAGES/Assembly_19.jpg" width="300">
</p> 

##### STEP 2: Push the magnet in the hole

<p align="center">
<img src="./IMAGES/Assembly_21.jpg" width="300">
</p> 

##### STEP 3: Repeat this for all 3 magnets 

<p align="center">
<img src="./IMAGES/Assembly_22.jpg" width="300">
</p> 

##### STEP 4: Add the sample tray to the assembly

<p align="center">
<img src="./IMAGES/Assembly_23.jpg" width="300">
</p> 


-----------------------

### STEP 4. Build the optical assembly

The last part is the microscope optical setup. It hosts the Z-stage, the folding mirrors and the eyepiece. 

##### Partslist 
|  Type | Details  |  Price | Link  |
|---|---|---|---|
| 2x Mirror  | PF10-03-P01	Ø1" Protected Silver Mirror |  50 € | [Thorlabs](https://www.thorlabs.com/thorproduct.cfm?partnumber=PF10-03-P01)  |
| Objective Lens | BRESSER DIN-Objektiv 60x, NA 0.85, 160/0.17 |  45 € | [Ebay](https://www.ebay.de/itm/BRESSER-DIN-Objektiv-60x/112674628997)  |
| Longpass (640)  |  Various |  200 € | [Chroma]()  |
| Ocular | MIKROSKOP OKULAR PAAR  PERIPLAN H 10 X  LEITZ WETZLAR GERMANY  |  8 € | [Ebay](https://www.ebay.de/itm/MIKROSKOP-OKULAR-PAAR-PERIPLAN-H-10-X-LEITZ-WETZLAR-GERMANY/133235531027)  |
| Optical Assembly | 3D Printed: Holding the Z-stage, Mirrors and Eyepiece |  8 € | [STL](./STL/cellSTORM_v5.dwg_cellSTORM_v5_opticalstage.stl)  |
| Screws | 4 * M3x20,  4 * M4x20, 4 x M3 nuts,  some small Philips screws | 1 € | [shelf]()  |
| Cover | 3D Printed: Cover for the optical assembly  |  5 € | [STL](./STL/cellSTORM_v5.dwg_cellSTORM_v5_microscope_cover.stl)  |
| Z-Stage | 3D Printed: Felxure stage for objective lens  |  5 € | [STL](./STL/cellSTORM_v5.dwg_cellSTORM_v5_focussingunit_1.stl)  |
| Z-Stage objective adapter | 3D Printed: Objective adapter for z-stage |  1 € | [STL](./STL/cellSTORM_objective_thread.stl)  |
| Filter adapter  | 3D Printed: Filter adapter for 25mm filter |  1 € | [STL](./STL/cellSTORM_v5.dwg_10_Filteslide_25mm.stl)  |


##### STEP 1: Get the parts ready

<p align="center">
<img src="./IMAGES/Assembly_24.jpg" width="300">
</p> 

##### STEP 2: Mount the objective lens in the objective slide and add a short M3 screw

<p align="center">
<img src="./IMAGES/Assembly_25.jpg" width="300">
</p> 

##### STEP 3: Slide in the Z-stage into the optical assembly

<p align="center">
<img src="./IMAGES/Assembly_26.jpg" width="300">
</p> 

##### STEP 4: Fix the Z-stage with some M3 screws

<p align="center">
<img src="./IMAGES/Assembly_27.jpg" width="300">
</p> 

##### STEP 4.1: Fix the Z-stage with some M3 screws

<p align="center">
<img src="./IMAGES/Assembly_28.jpg" width="300">
</p>

##### STEP 4.2: ALTERNATIVE: Fix the Z-stage with some M3 screws

<p align="center">
<img src="./IMAGES/Assembly_29.jpg" width="300">
</p>

##### STEP 5: Add the micrometer screw 

<p align="center">
<img src="./IMAGES/Assembly_30.jpg" width="300">
</p>

##### STEP 6: Fix the micrometer screw

<p align="center">
<img src="./IMAGES/Assembly_31.jpg" width="300">
</p>

##### STEP 7: Add M3 screws to the Z-stage

<p align="center">
<img src="./IMAGES/Assembly_32.jpg" width="300">
</p>

##### STEP 8: Add rubber band to the assembly

<p align="center">
<img src="./IMAGES/Assembly_33.jpg" width="300">
</p>

##### STEP 9: Add objective lens by sliding it in

<p align="center">
<img src="./IMAGES/Assembly_34.jpg" width="300">
</p>

##### STEP 10: Add the mirrors in the two holes

<p align="center">
<img src="./IMAGES/Assembly_35.jpg" width="300">
</p>

##### STEP 11: Add the cover plate

<p align="center">
<img src="./IMAGES/Assembly_36.jpg" width="300">
</p>

##### STEP 12: Done!

<p align="center">
<img src="./IMAGES/Assembly_37.jpg" width="300">
</p>

##### Optional (Problem): Make sure the micrometers of the stages look like this (orientation)

<p align="center">
<img src="./IMAGES/Assembly_39.jpg" width="300">
</p>



### STEP 5. Finalize the setup

The last part is the microscope optical setup. It hosts the Z-stage, the folding mirrors and the eyepiece. 

##### Partslist 
|  Type | Details  |  Price | Link  |
|---|---|---|---|
| Sticky Tape |  Various |  1 € | [shelf]()  |
| Motor | 12V 28BYJ-48 Step Motor XH-5P Stock |  3 € | [Eckstein](https://eckstein-shop.de/12V-28BYJ-48-Step-Motor-XH-5P-Stock?curr=EUR&gclid=Cj0KCQjwoPL2BRDxARIsAEMm9y8fZPWrWBwHugC5pmkUBT4a2-Mu1Nse5iA_QdGtwm0U20FVvchuxZoaAimaEALw_wcB)  |
| Screws | 4 * M3x20,  4 * M4x20, 4 x M3 nuts,  some small Philips screws | 1 € | [shelf]()  |
| Motor adapter | 3D Printed: Arm to hold the focusing motor |  5 € | [STL](./STL/cellSTORM_v5.dwg_cellSTORM_v5_focusmotormount.stl)  |
| Cellphone Plate | 3D Printed: Mount for optical assembly and cellphone |  5 € | [STL](./STL/cellSTORM_v5.dwg_cellSTORM_v5_cellphonestage.stl)  |
| Pulley for micrometer | 3D Printed: Pulley for the autofocus |  1 € | [STL](./STL/cellSTORM_v5.dwg_BD-DRIVE_PULLEY.stl)  |
| Spacer  | 3D Printed: Rise the distance between the XY stage to adjust the height for the cellphone |  3 € | [STL](./STL/cellSTORM_v5_cellphonespacer.stl)  |




##### STEP 1: Mount the cellphone adapter on the upper XY stage 

<p align="center">
<img src="./IMAGES/Assembly_40.jpg" width="300">
</p> 

##### STEP 2: Mount the optical path to the cellphone mount using M3 screws

<p align="center">
<img src="./IMAGES/Assembly_42.jpg" width="300">
</p> 

##### STEP 2.1: Mount the optical path to the cellphone mount using M3 screws

<p align="center">
<img src="./IMAGES/Assembly_43.jpg" width="300">
</p> 

##### STEP 3: Mount the cellphone (optional: add blutec to "glue" it to the surface)

<p align="center">
<img src="./IMAGES/Assembly_44.jpg" width="300">
</p> 

##### STEP 3.1: Mount the cellphone

<p align="center">
<img src="./IMAGES/Assembly_45.jpg" width="300">
</p> 


##### STEP 4: Finalizing everything

<p align="center">
<img src="./IMAGES/Assembly_46.jpg" width="300">
</p> 

##### STEP 5: Add the autofocus motor adapter

<p align="center">
<img src="./IMAGES/Assembly_47.jpg" width="300">
</p> 

##### STEP 5: Add the motor and fix it using M3 screws, add the pulley (may look different)

<p align="center">
<img src="./IMAGES/Assembly_48.jpg" width="300">
</p> 

##### STEP 6: Add a sticky tape as a belt alternative

<p align="center">
<img src="./IMAGES/Assembly_50.jpg" width="300">
</p> 

##### STEP 7: Finalize autofocus mechanism

<p align="center">
<img src="./IMAGES/Assembly_51.jpg" width="300">
</p> 

##### FINAL STEP: Grab a coffee and relax. 

<p align="center">
<img src="./IMAGES/Assembly_52.jpg" width="300">
</p> 


## Contribute

If you find any problem, please file an issue so that we can solve it! Thanks! :-) 