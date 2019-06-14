mytimeseries = readtimeseries('C:\Users\diederichbenedict\Dropbox\Dokumente\Promotion\PROJECTS\STORMoChip\PYTHON\2019-06-13_15.07.46_Fluctuation_Cardio_Mayoblast_Cellphone_Largelens.mp4.tif');

%%
myshift = {};
myflucti = {};
nframes = size(mytimeseries,3);
for iframe = 1:nframes-1
    myshift{iframe} = findshift(squeeze(mytimeseries(:,:,iframe)), squeeze(mytimeseries(:,:,iframe+1)));
    myflucti{iframe} = shift(squeeze(mytimeseries(:,:,iframe+1)), myshift{iframe});
    iframe
end

myflucti_mat = cat(3, myflucti{:});


