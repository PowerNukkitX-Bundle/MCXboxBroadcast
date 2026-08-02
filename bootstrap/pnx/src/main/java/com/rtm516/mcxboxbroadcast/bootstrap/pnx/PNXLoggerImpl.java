package com.rtm516.mcxboxbroadcast.bootstrap.pnx;

import com.rtm516.mcxboxbroadcast.core.Logger;

public final class PNXLoggerImpl implements Logger {
    private final org.powernukkitx.utils.Logger logger;
    private final String prefix;

    public PNXLoggerImpl(org.powernukkitx.utils.Logger logger) {
        this(logger, "");
    }

    private PNXLoggerImpl(org.powernukkitx.utils.Logger logger, String prefix) {
        this.logger = logger;
        this.prefix = prefix;
    }

    @Override
    public void info(String message) {
        logger.info(format(message));
    }

    @Override
    public void warn(String message) {
        logger.warning(format(message));
    }

    @Override
    public void error(String message) {
        logger.error(format(message));
    }

    @Override
    public void error(String message, Throwable ex) {
        logger.error(format(message), ex);
    }

    @Override
    public void debug(String message) {
        logger.debug(format(message));
    }

    @Override
    public Logger prefixed(String prefix) {
        return new PNXLoggerImpl(logger, prefix);
    }

    private String format(String message) {
        return prefix.isEmpty() ? message : "[" + prefix + "] " + message;
    }
}
