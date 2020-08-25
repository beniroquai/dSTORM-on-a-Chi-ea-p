% align GFP and AF647 channel
filename_gfp = './2020_08_14-SARS_VLP_DualColour_GFP_smallchip_HuaweiP20Pro_1_20s_ISO3200_mp4/SUM/SUM_02_GFP_2020-08-14 16.59.42-2.tif';
filename_af647 = './2020_08_14-SARS_VLP_DualColour_GFP_smallchip_HuaweiP20Pro_1_20s_ISO3200_mp4/SUM/SUM_02_AF647_2020-08-14 16.59.42-2.tif';

filename_gfp = './SRRF/SRRF_GFP_MOV_2020_08_17_15_21_12-3.tif - SRRF.tif';
filename_af647 = './SRRF/SRRF_AF647_MOV_2020_08_17_15_21_12-2.tif - drift corrected - SRRF.tif';

% read frames
images_gfp = readim(filename_gfp);
images_af647 = readim(filename_af647);


if(1)
    % normalize
    images_gfp(images_gfp==-Inf)=0;
    images_af647(images_af647==-Inf)=0;
    % resample
    images_gfp = resample(images_gfp, 0.25);
    images_af647 = resample(images_af647, 0.25);
    
    images_gfp = images_gfp-min(images_gfp);
    images_gfp = images_gfp/max(images_gfp);
    images_af647 = images_af647-min(images_af647);
    images_af647 = images_af647/max(images_af647);
    
    % log
    %images_gfp = images_gfp^.1;
    %images_af647 = images_af647^.1;
    % threshold
    %images_gfp = images_gfp*(images_gfp>(mean(images_gfp)*1.5));
    %images_af647 = images_af647*(images_af647>(mean(images_af647)*1.5));
    
end


mystack = cat(3, images_gfp, images_af647)

% detect the coordinates of the box
disp('select outer frame')
mycoordinates = getFrameCoordinates(mystack);

mydiff = mycoordinates(1:2:end,1:2)-mycoordinates(2:2:end,1:2);
shiftxy = mean(dip_image(mydiff),[],2);

myimageresult = cat(3, affine_trans(images_gfp, [1,1], int16(shiftxy), 0), images_af647)

% write image
for x = 0:1
        imwrite(double(myimageresult(:,:,x)),strcat(filename_gfp,'_GFP_AF647_colour.tif'), 'Compression', 'none','WriteMode', 'append');
end

writeim((myimageresult), strcat(filename_gfp,'_GFP_AF647_colour.tif'))