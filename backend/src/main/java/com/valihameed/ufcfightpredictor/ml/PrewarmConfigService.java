package com.valihameed.ufcfightpredictor.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PrewarmConfigService {
    private final AtomicBoolean enabled;
    private final long lookaheadHours;

    public PrewarmConfigService(@Value("${prewarm.enabled:true}") boolean enabled,
                                @Value("${prewarm.lookahead-hours:3}") long lookaheadHours) {
        this.enabled = new AtomicBoolean(enabled);
        this.lookaheadHours = lookaheadHours;
    }

    public boolean isEnabled() { return enabled.get(); }

    public void setEnabled(boolean v) { enabled.set(v); }

    public long getLookaheadHours() { return lookaheadHours; }
}
