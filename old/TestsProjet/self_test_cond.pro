programme self_test_cond :
const m=10;
var ent n,j ;
debut
     n := m;
     j := 5;
     cond 
         n=5 : n:=1 ,
         n =7 : n:=2 ,
         n = 10 : 
             cond 
                 j = 5 : n:= 3,
                 j = 10 : n:= 4
                 aut n:= 5
             fcond; 
     fcond;
fin  