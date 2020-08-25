function [mycoordinates] = getFrameCoordinates(input)
 

mysize=size(squeeze(input));

%% create a figure handle and select the coordinates
%fh=dipshow(input(:,:,1)); % find edges of CC signal
fh=dipshow((input)); % find edges of CC signal

diptruesize(fh, 100);
%fh=dipshow(abs(AllSubtractFT(:,:,0))^.1) % find edges of CC signal
fprintf('Select 4 coordinates in each figure');
fprintf('Order: Upper-Left Corner, Upper-Right Corner, Lower-Right Corner, Lower-Left Corner')
mycoordinates = dipgetcoords(fh,6);
fprintf('Thank you :-)')
