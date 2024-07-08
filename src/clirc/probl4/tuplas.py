## Código de referencia do livro https://introtcs.org/public/lec_04_code_and_data.html#listoftuplesrepsec
## Disponível em https://github.com/boazbk/tcscode/blob/master/Chap_05_code_and_data.ipynb

xorcode = r'''temp_0 = NAND(X[0],X[1])
temp_1 = NAND(X[0],temp_0)
temp_2 = NAND(X[1],temp_0) 
Y[0] = NAND(temp_1,temp_2)
'''
len(xorcode)

code = r'''temp_0 = NAND(X[0],X[1])
t1 = NAND(temp_0,temp_0)
notx0 = NAND(X[0],X[0])
temp_1 = NAND(notx0,X[2])
t2 = NAND(temp_1,temp_1)
temp_2 = NAND(t1,t1)
temp_3 = NAND(t2,t2)
Y[0] = NAND(temp_2,temp_3)'''

def code2rep(code):
    """Map NAND-CODE to the list-of-triples representation."""
    inputs = [] ; workspace = [] ; outputs = []
    def parse(line): # extract 3 variables from line of code
        return line[:line.find("=")].strip(), line[line.find("(")+1:line.find(",")].strip(),  line[line.find(",")+1:line.find(")")].strip()
        
    def addvar(var): # add variable to inputs/outputs/workspace lists
        nonlocal inputs,workspace,outputs
        if var[0]=='X': 
            if not var in inputs: inputs += [var]
        elif var[0]=='Y': 
            if not var in outputs: outputs += [var]
        elif not var in workspace:
            workspace += [var] 
        
    # add all variables
    for line in code.split('\n'):
        for var in parse(line):
            addvar(var)
    
    variables = sorted(inputs) + workspace + sorted(outputs)
    print(variables)
    L = [] # list of triples 
    for line in code.split('\n'):
        foo,bar,blah = parse(line)
        L += [[variables.index(foo),variables.index(bar),variables.index(blah)]]
    
    return (len(inputs),len(outputs),L) 

print(code2rep(code))