import shutil
import subprocess
import sys
import os

def checkout(project:str,path:str):
    subject, bug_id = project.split('_')

    if os.path.exists(f'{path}/{project}'):
        shutil.rmtree(f'{path}/{project}')

    if not os.path.exists(f'{subject}-{bug_id}.tar.gz'):
        subprocess.run(['wget',f'https://github.com/ali-ghanbari/d4j-{subject.lower()}/raw/master/{subject}-{bug_id}.tar.gz'])
    subprocess.run(['tar','-xf',f'{subject}-{bug_id}.tar.gz'])
    if subject=='Lang':
        subprocess.run(['rm','-rf','src/test/java/org/apache/commons/lang3/reflect/TypeUtilsTest.java'],cwd=f'{bug_id}')
    subprocess.run(['mv',f'{bug_id}',f'{path}/{project}'])

    result=subprocess.run(['mvn','compile'],cwd=f'{path}/{project}')
    if result.returncode != 0:
        with open(f'{path}/{project}/pom.xml','r') as f:
            lines=f.readlines()
            for i,line in enumerate(lines):
                if line.strip()=='<url>http://repo1.maven.org/maven2</url>':
                    lines[i]='<url>https://repo1.maven.org/maven2</url>'
                    break
        with open(f'{path}/{project}/pom.xml','w') as f:
            f.writelines(lines)
        result=subprocess.run(['mvn','compile'],cwd=f'{path}/{project}')

        if result.returncode != 0:
            exit(1)

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print('Usage: python3 d4j-checkout.py <project_name> <target_path')
        exit(1)
    checkout(sys.argv[1],sys.argv[2])