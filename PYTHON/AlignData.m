mytimeseries = readtimeseries('C:\Users\diederichbenedict\Dropbox\Dokumente\Promotion\PROJECTS\STORMoChip\PYTHON\2019-06-13_15.07.46_Fluctuation_Cardio_Mayoblast_Cellphone_Largelens.mp4.tif');

%%
myshift = {};
myflucti = {};
nframes = size(mytimeseries,3);
firstframe = extract(squeeze(mytimeseries(:,:,0)), [200,200]);
for iframe = 1:nframes-1
    secondframe = extract(squeeze(mytimeseries(:,:,iframe)), [200,200]);
    myshift{iframe} = findshift(firstframe, secondframe,'ffts');
    myflucti{iframe} = shift(squeeze(mytimeseries(:,:,iframe+1)), myshift{iframe});
    iframe
end

myflucti_mat = double(cat(3, myflucti{:}));
save('myflucti_mat.TIFF','myflucti_mat','-v7.3')

