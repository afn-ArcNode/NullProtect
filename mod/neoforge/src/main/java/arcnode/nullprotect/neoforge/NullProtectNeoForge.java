package arcnode.nullprotect.neoforge;

import net.neoforged.fml.common.Mod;

import arcnode.nullprotect.NullProtect;

@Mod(NullProtect.MOD_ID)
public final class NullProtectNeoForge {
    public NullProtectNeoForge() {
        // Run our common setup.
        NullProtect.init();
    }
}
