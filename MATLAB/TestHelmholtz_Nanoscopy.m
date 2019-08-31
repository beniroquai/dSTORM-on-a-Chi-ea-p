% performs a 2D simulation, assuming cylinder symmertry along Z

mysize=[1024 512];
nEmbb = 1.0
myN = newim(mysize,'scomplex')+nEmbb;

SlabX=260;
SlabW=50;
SlabN=1.52 + 0.0*i;

switch (1)
    case 1,
    myN=insertSphere(myN,[SlabX 200],10,SlabN);  % small radius 10
    case 2,
    SlabN=1.01;  % relative refractive index just a little higher
    SlabI=1.00;
    myN=insertScatterSlab(myN,120,size(myN,1)-100-121,SlabI,SlabN,200,20);
    case 3,
    myN=insertSlab(myN,SlabX,SlabW,SlabN);
    N_AR=sqrt(1.0*real(SlabN));  % ideal reflective index of an AR coating
    myXX=xx(myN,'corner');
    myN(abs(myXX-SlabX)==SlabW)=N_AR;  % AR coating
    case 4, % grating Slab
        SlabN=1.02;  % relative refractive index just a little higher
        SlabW=600;
        SlabN2=1.0;
        doRect=0;
        G=20.0;  % grating constant
        myN=insertGratingSlab(myN,SlabX,SlabW,SlabN,SlabN2,G,doRect);
    case 5, % grating Slab with "blazing" (ramp in n)
        SlabN=1.0;  % relative refractive index just a little higher
        SlabW=600;
        SlabN2=1.02;
        doRect=2;
        G=20.0;  % grating constant
        myN=insertGratingSlab(myN,SlabX,SlabW,SlabN,SlabN2,G,doRect);
end

myN

k0=0.25/abs(SlabN);
myN=insertPerfectAbsorber(myN,0,100,-1,k0);
myN=insertPerfectAbsorber(myN,size(myN,1)-100,100,1,k0);

kx = 0.3;
myWidth=60;
mySrc=insertSrc(mysize,myWidth,[101 80],kx);
%%

% res=HelmholtzSolver(cuda(myN),cuda(mySrc),[],k0,[],1000,500)

% resB=Born(myN,mySrc,k0)

res=HelmholtzSolver(myN,mySrc,[],k0,nEmbb,[1 1],10,20)
% display energy propagation in k-space (for pendellösung)

tmp=abssqr(dip_fouriertransform(res,'forward',[0 1]))

order0 = sum(tmp(:,278:278+5),[],2);
order1 = sum(tmp(:,228:228+5),[],2);
plot(order0);hold on;plot(order1,'g')

%%
res2=HelmholtzSolver(myN,mySrc,[],k0,resB*28.6,[],500)

cat(3,resB,res,res2)

abssqr(res(105,:))/max(abssqr(res(105,:)))

sum(abssqr(res),[],2)

abssqr(res(:,200))

%%
res2=HelmholtzSolver(myN,mySrc,[],k0,res)

