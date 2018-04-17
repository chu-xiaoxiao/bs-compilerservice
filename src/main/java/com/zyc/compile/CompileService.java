package com.zyc.compile;

import com.fasterxml.jackson.databind.util.ArrayIterator;
import javassist.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

/**
 * Created by YuChen Zhang on 18/01/08.
 */
@Service
public class CompileService {
    private static final String CURRENTDIR = System.getProperty("user.dir");
    private static final Random random = new Random(new Date().getTime());
    private static final Logger LOGGER = LogManager.getFormatterLogger(CompileService.class);

    private String mainFileName;
    private String fileDir;

    /**
     * 编译主方法
     * @param codes
     * @return
     */
    public String compile(Iterable<String> codes){
        List<File> files = new ArrayList<File>();
        StringBuffer result = new StringBuffer();
        for(String temp : codes){
            try {
                files.add(createFileFromCode(temp));
            } catch (IOException e) {
                e.printStackTrace();
                result.append("编译错误\n"+e.getMessage());
            }
        }
        try {
            result.append(compileFile(files));
        } catch (IOException e) {
            e.printStackTrace();
            result.append("编译错误\n"+e.getMessage());
            return result.toString();
        }
        try {
            result.append("\n"+execute(files));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return result.toString();
    }


    public File createFileFromCode(String code) throws IOException {
        //分割代码
        String[] codes = code.split(";");
        //获取代码包名
        String packageName1 = codes[0].split(" ")[1];
        //根据代码包名组织代码存放临时路径
        String packageName;
        packageName = packageName1.replace(".", "\\");
        LOGGER.debug("组织临时文件路径");
        String className = "";
        for (String temp : codes) {
            if (temp.contains("public class")) {
                className = temp.split("public class")[1].split("\\{")[0].trim();
            }
            if(temp.contains("public static void main")){
                mainFileName=packageName1+"."+className;
            }
        }
        LOGGER.debug("组织类文件");
        fileDir = CURRENTDIR + "/temp" + Math.abs(random.nextInt()) + "/";
        LOGGER.debug("生成临时文件路径" + fileDir);
        //写出java文件
        String filename = fileDir + packageName + "/" + className + ".java";
        LOGGER.debug("生成" + filename);
        File file = new File(filename);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        FileWriter fw = new FileWriter(file);
        fw.write(code);
        fw.flush();
        fw.close();
        //编译java文件（待补充）
        return file;
    }


    private void deleteTempDir(File file) {
        //记录删除文件数量
        File[] files = file.listFiles();
        Integer count;
        for (File temp : files) {
            if (temp.isDirectory()) {
                deleteTempDir(temp);
            } else if (temp.isFile()) {
                temp.delete();
                LOGGER.debug("删除临时文件" + temp);
            }
        }
        file.delete();
        LOGGER.debug("删除临时文件夹" + file);
    }

    public String getError(DiagnosticCollector diagnosticCollector) {
        StringBuffer result = new StringBuffer();
        List<Diagnostic> diagnostics = diagnosticCollector.getDiagnostics();
        for (Diagnostic diagnostic : diagnostics) {
            result.append("Code:");
            result.append(diagnostic.getCode());
            result.append("\r\n");
            result.append("Kind:");
            result.append(diagnostic.getKind());
            result.append("\r\n");
            result.append("Position:");
            result.append(diagnostic.getPosition());
            result.append("\r\n");
            result.append("Start Position:");
            result.append(diagnostic.getStartPosition());
            result.append("\r\n");
            result.append("End Position:");
            result.append(diagnostic.getEndPosition());
            result.append("\r\n");
            result.append("Source:");
            result.append(diagnostic.getSource());
            result.append("\r\n");
            result.append("Message:");
            result.append(diagnostic.getMessage(null));
            result.append("\r\n");
        }
        return result.toString();
    }

    public String compileFile(List<File> files) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        StringWriter stringWriter = new StringWriter();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // 建立DiagnosticCollector对象
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        // 建立用于保存被编译文件名的对象
        // 每个文件被保存在一个从JavaFileObject继承的类中
        Iterable<? extends JavaFileObject> compilationUnits = fileManager
                .getJavaFileObjectsFromFiles(files);
        JavaCompiler.CompilationTask task = compiler.getTask(stringWriter, fileManager,
                diagnostics, null, null, compilationUnits);
        // 编译源程式
        boolean success = task.call();
        fileManager.close();
        stringBuffer.append((success) ? "编译成功" : "编译失败");
        stringBuffer.append(stringWriter.getBuffer().toString());
        stringBuffer.append(getError(diagnostics));
        return stringBuffer.toString();
    }

/*    public String execute(Iterable<File> files) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        File out = new File(fileDir+"out.txt");
        StringBuffer stringBuffer = new StringBuffer();
        PrintStream printStream = new PrintStream(out);
        final boolean[] flag = {false};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.setOut(printStream);
                //获取类加载路径
                List<URL> urls = new ArrayList<URL>();
                try {
                    urls.add(new File(fileDir).toURI().toURL());
                    URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[0]));
                    //加载主类
                    Class mainClass = Class.forName(mainFileName,true, urlClassLoader);
                    //反射main
                    Method method = mainClass.getMethod("main",String[].class);
                    //执行main方法
                    method.invoke(mainClass.newInstance(),new Object[]{new String[0]});
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(out));
                    //组织结果
                    String line = null;
                    for(;(line=bufferedReader.readLine())!=null;){
                        stringBuffer.append(line);
                    }
                    bufferedReader.close();
                    urlClassLoader.close();
                    flag[0] = true;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.run();
        for(int i = 0;i<3;i++){
            try {
                if(flag[0]){
                    printStream.close();
                    deleteTempDir(new File(fileDir));
                    return stringBuffer.toString();
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        printStream.close();
        //删除临时文件
        deleteTempDir(new File(fileDir));
        LOGGER.error("运行超时");
        return "运行超时";
    }*/
    public String execute(Iterable<File> files) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        File out = new File(fileDir+"out.txt");
        StringBuffer stringBuffer = new StringBuffer();
        PrintStream printStream = new PrintStream(out);
        final boolean[] flag = {false};
        System.setOut(printStream);
        //获取类加载路径
        List<URL> urls = new ArrayList<URL>();
        urls.add(new File(fileDir).toURI().toURL());
        URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[0]));
        //加载主类
        Class mainClass = Class.forName(mainFileName,true, urlClassLoader);
        //反射main
        Method method = mainClass.getMethod("main",String[].class);
        //执行main方法
        method.invoke(mainClass.newInstance(),new Object[]{new String[0]});
        BufferedReader bufferedReader = new BufferedReader(new FileReader(out));
        //组织结果
        String line = null;
        for(;(line=bufferedReader.readLine())!=null;){
            stringBuffer.append(line);
        }
        bufferedReader.close();
        urlClassLoader.close();
        printStream.close();
        //删除临时文件
        deleteTempDir(new File(fileDir));
        return stringBuffer.toString();
    }
}
