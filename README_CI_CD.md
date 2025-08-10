# Android CI/CD Pipeline 配置说明

## 概述
这是一个**最小成本**的Android CI/CD pipeline，包含基本的测试、构建和发布功能，无需复杂的代码质量检查工具。

## 功能特性
- ✅ 单元测试执行
- ✅ Debug/Release APK构建
- ✅ Release APK自动签名
- ✅ GitHub Release自动创建
- ✅ 分支策略：develop分支构建Debug版本，main分支构建Release版本

## 配置步骤

### 1. 创建GitHub Secrets
在GitHub仓库的Settings > Secrets and variables > Actions中添加以下secrets：

```
ANDROID_KEYSTORE_BASE64    # 你的keystore文件的base64编码
KEY_ALIAS                  # keystore中的key别名
KEYSTORE_PASSWORD          # keystore密码
KEY_PASSWORD              # key密码
```

### 2. 生成keystore的base64编码
```bash
# 在本地执行
base64 -i your-release-key.keystore | tr -d '\n'
# 复制输出的内容到ANDROID_KEYSTORE_BASE64 secret
```

### 3. 分支策略
- **develop分支**: 推送到develop分支会触发Debug APK构建
- **main分支**: 推送到main分支会触发Release APK构建、签名和发布

### 4. 工作流程
1. 代码推送到develop/main分支
2. 自动运行单元测试
3. 构建对应的APK
4. 如果是main分支，自动签名并创建GitHub Release

## 文件结构
```
.github/
  workflows/
    android-ci-cd.yml    # CI/CD配置文件
app/
  build.gradle.kts       # 应用构建配置（已简化）
```

## 为什么选择最小成本方案？

### 传统CI/CD的问题：
- ❌ 需要配置复杂的代码质量检查工具（ktlint, detekt等）
- ❌ 需要创建额外的配置文件
- ❌ 增加维护成本和调试难度
- ❌ 可能因为工具版本兼容性问题导致构建失败

### 我们的解决方案：
- ✅ 只保留核心功能：测试、构建、签名、发布
- ✅ 无需额外配置文件
- ✅ 使用Android官方推荐的Gradle配置
- ✅ 快速部署，易于维护

## 注意事项
- 确保你的项目有基本的单元测试
- keystore文件必须正确配置
- 第一次运行可能需要较长时间来下载依赖
- 如果遇到构建失败，检查GitHub Secrets配置和本地构建是否正常

## 故障排除
如果遇到构建失败：
1. 检查GitHub Secrets是否正确配置
2. 查看Actions日志了解具体错误
3. 确保本地项目能够正常构建（运行 `./gradlew assembleDebug`）
4. 检查Gradle版本兼容性

## 扩展建议（可选）
如果后续需要添加更多功能，可以考虑：
- 添加代码覆盖率检查（JaCoCo）
- 添加代码质量检查（ktlint）
- 添加安全检查
- 添加性能测试

但建议先让基础pipeline稳定运行，再逐步添加功能。 