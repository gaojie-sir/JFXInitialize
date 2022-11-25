package com.jack.jfx.handler;


import com.jack.jfx.abs.AbstractJavaFxApplicationSupport;
import com.jack.jfx.annotation.FXMLView;
import com.jack.jfx.annotation.JFXComponent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 类加载工具
 *
 * @author gj
 */
public class ComponentLoader {
	/**
	 * 需要过滤的文件类型
	 */
	private static final String CLASS_FILE_SUFFIX = ".class";

	/**
	 * 本地class文件路径
	 */
	private static List<Class<?>> classes = new ArrayList<>();


	private static String getClassName(String url) {
		if (!url.endsWith(CLASS_FILE_SUFFIX)) {
			return null;
		}
		url = url.replaceAll("/", ".").
				replaceAll("\\\\", ".").
				replace(CLASS_FILE_SUFFIX, "");
		return url;
	}


	/**
	 * 注册实现了 @Report 注解 的类
	 *
	 * @param packageName
	 */
	public static void loadClasses(String packageName) {
		getClasses(packageName);
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		if (contextClassLoader != null) {
			try {
				Field classesFiled = ClassLoader.class.getDeclaredField("classes");
				classesFiled.setAccessible(true);
				List<Class<?>> allClasses = (List<Class<?>>) classesFiled.get(contextClassLoader);
				List<Class<?>> collect = new ArrayList<>(allClasses);
				List<Class<?>> hnfClasses = collect.stream().filter(classes -> classes.getName().contains("hnf")).collect(Collectors.toList());
				classes.addAll(hnfClasses);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				Logger.getGlobal().log(Level.SEVERE, null, e);
				e.printStackTrace();
			}
		}
		List<Class<?>> collect = classes.stream().distinct().collect(Collectors.toList());
		//开始加载界面
		for (Class<?> aClass : collect) {
			registerClassToAppContext(aClass);
		}
	}

	/**
	 * 注册对象到上下文
	 *
	 * @param aClass
	 */
	public static void registerClassToAppContext(Class<?> aClass) {
		if (aClass == null || aClass.getPackage() == null) {
			return;
		}
		if (!aClass.getPackage().getName().startsWith("com.hnf")) {
			return;
		}
		if (AbstractJavaFxApplicationSupport.getBean(aClass) == null) {
			JFXComponent jfxComponent = aClass.getAnnotation(JFXComponent.class);
			FXMLView fxmlView = aClass.getAnnotation(FXMLView.class);
			if (jfxComponent != null || fxmlView != null) {
				try {
					if (AbstractJavaFxApplicationSupport.getBean(aClass) == null) {
						Object bean = aClass.newInstance();
						AbstractJavaFxApplicationSupport.addBean(aClass, bean);
					}
				} catch (Exception e) {
					Logger.getGlobal().log(Level.SEVERE, null, e);
					e.printStackTrace();
					System.out.println(aClass);
				}
			}
		}
	}


	/**
	 * 从包package中获取所有的Class
	 *
	 * @param
	 * @return
	 */
	private static void getClasses(String packageName) {
		// 是否循环迭代
		boolean recursive = true;
		// 获取包的名字 并进行替换
		String packageDirName = packageName.replace('.', '/');
		// 定义一个枚举的集合 并进行循环来处理这个目录下的things
		Enumeration<URL> dirs;
		try {
			dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
			// 循环迭代下去
			while (dirs.hasMoreElements()) {
				// 获取下一个元素
				URL url = dirs.nextElement();
				// 得到协议的名称
				String protocol = url.getProtocol();
				// 如果是以文件的形式保存在服务器上
				if ("file".equals(protocol)) {
					// 获取包的物理路径
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					// 以文件的方式扫描整个包下的文件 并添加到集合中
					findAndAddClassesInPackageByFile(packageName, filePath, recursive);
				} else if ("jar".equals(protocol)) {
					// 如果是jar包文件
					// 定义一个JarFile
					JarFile jar;
					try {
						// 获取jar
						jar = ((JarURLConnection) url.openConnection()).getJarFile();
						// 从此jar包 得到一个枚举类
						Enumeration<JarEntry> entries = jar.entries();
						// 同样的进行循环迭代
						while (entries.hasMoreElements()) {
							// 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
							JarEntry entry = entries.nextElement();
							String name = entry.getName();
							// 如果是以/开头的
							if (name.charAt(0) == '/') {
								// 获取后面的字符串
								name = name.substring(1);
							}
							// 如果前半部分和定义的包名相同
							if (name.startsWith(packageDirName)) {
								int idx = name.lastIndexOf('/');
								// 如果以"/"结尾 是一个包
								if (idx != -1) {
									// 获取包名 把"/"替换成"."
									packageName = name.substring(0, idx).replace('/', '.');
								}
								// 如果可以迭代下去 并且是一个包
								// 如果是一个.class文件 而且不是目录
								if (name.endsWith(".class") && !entry.isDirectory()) {
									// 去掉后面的".class" 获取真正的类名
									String className = name.substring(packageName.length() + 1, name.length() - 6);
									try {
										String classPath = packageName + '.' + className;
										//使用当前线程类加载器加载
										ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
										// 添加到classes
										Class<?> aClass = contextClassLoader.loadClass(classPath);
										classes.add(aClass);
//                                        classes.add(Class.forName(classPath));
									} catch (ClassNotFoundException e) {
										Logger.getGlobal().log(Level.SEVERE, null, e);
									}
								}
							}
						}
					} catch (IOException e) {
						Logger.getGlobal().log(Level.SEVERE, null, e);
					}
				}
			}
		} catch (IOException e) {
			Logger.getGlobal().log(Level.SEVERE, null, e);
		}
	}

	/**
	 * 以文件的形式来获取包下的所有Class
	 *
	 * @param packageName
	 * @param packagePath
	 * @param recursive
	 */
	private static void findAndAddClassesInPackageByFile(String packageName, String packagePath,
														 final boolean recursive) {
		// 获取此包的目录 建立一个File
		File dir = new File(packagePath);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}
		// 如果存在 就获取包下的所有文件 包括目录
		// 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
		File[] dirFiles = dir.listFiles(file -> (recursive && file.isDirectory()) || (file.getName().endsWith(".class")));
		// 循环所有文件
		for (File file : dirFiles) {
			// 如果是目录 则继续扫描
			if (file.isDirectory()) {
				findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(),
						recursive);
			} else {
				// 如果是java类文件 去掉后面的.class 只留下类名
				String className = file.getName().substring(0, file.getName().length() - 6);
				try {
					// 添加到集合中去
					classes.add(Class.forName(packageName + '.' + className));
				} catch (ClassNotFoundException e) {
					Logger.getGlobal().log(Level.SEVERE, null, e);
				}
			}
		}
	}


}
