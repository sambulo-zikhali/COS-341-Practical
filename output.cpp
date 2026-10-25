/* begin of the non-executable code-mine
begin of code-mine for Function v2
Parameters for inlining:
int v3 ;
Body of the procedure:
 
int t0 ;
int t1 ;
int t2 ;
t1 = v3 ;
t2 = 1 ;
t0 = t1 + t2 ;
v3 = t0 ;
;
int v2r3 ;
v2r3 = v3 ;
end of code-mine for Function v2
The result is found in the return value v2r3

end of the non-executable code-mine */
/* begin of executable C code */
#include <iostream>
int main()
{
int v0 ;
int v1 ;
 
int t4 ;
t4 = 0 ;
v0 = t4 ;
int t5 ;
int p6 ;
p6 = v0 ;
int v3k7 ;
v3k7 = p6 ;
 
int t8 ;
int t9 ;
int t10 ;
t9 = v3k7 ;
t10 = 1 ;
t8 = t9 + t10 ;
v3k7 = t8 ;
int r11 ;
r11 = v3k7 ;
t5 = r11 ;
v1 = t5 ;
int t12 ;
t12 = v1 ;
std::cout << t12 ;
/*if */
int t16 ;
int t17 ;
t16 = v0 ;
t17 = 0 ;
if ( t16 == t17 ) goto L13 ;
goto L14 ;
L14 : /*else */ ;
std::cout << "notz" ;
goto L15 ;
L13 : /*then */ ;
std::cout << "isz" ;
L15 : /*continue */ ;
L18 : /*while */ ;
int t21 ;
int t22 ;
t21 = v0 ;
t22 = 5 ;
if ( t21 < t22 ) goto L20 ;
goto L19 ;
L20 : /*do */ ;
int t23 ;
int t24 ;
int t25 ;
t24 = v0 ;
t25 = 1 ;
t23 = t24 + t25 ;
v0 = t23 ;
int t26 ;
t26 = v0 ;
std::cout << t26 ;
goto L18 ;
L19 : /*exit */ ;
return 0 ;
}
/* end of executable C */
