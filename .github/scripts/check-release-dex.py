#!/usr/bin/env python3
"""Checks that the shrunk store build still has the constructors libraries call by reflection.

R8 removes a constructor that no code calls directly. A library that creates a class by
reflection needs keep rules for it; when they are missing, the debug build works and the store
build crashes. v0.2.1 crashed on start this way: WorkManager's old Room let R8 remove the
constructor of `WorkDatabase_Impl` (Q-09).

Usage: check-release-dex.py app/build/outputs/bundle/release/app-release.aab
"""
import struct
import sys
import zipfile

# Class (dex descriptor) -> why it needs a constructor with no parameters.
NEEDS_CONSTRUCTOR = {
    "Landroidx/work/impl/WorkDatabase_Impl;": "Room creates WorkManager's database by reflection (Q-09)",
}


def uleb128(data, offset):
    result = shift = 0
    while True:
        byte = data[offset]
        offset += 1
        result |= (byte & 0x7F) << shift
        shift += 7
        if byte < 0x80:
            return result, offset


class Dex:
    def __init__(self, data):
        self.data = data
        self.string_ids = self.u4(0x3C)
        self.type_ids = self.u4(0x44)
        self.proto_ids = self.u4(0x4C)
        self.method_ids = self.u4(0x5C)
        self.class_count = self.u4(0x60)
        self.class_defs = self.u4(0x64)

    def u4(self, offset):
        return struct.unpack_from("<I", self.data, offset)[0]

    def string(self, index):
        offset = self.u4(self.string_ids + 4 * index)
        _, offset = uleb128(self.data, offset)  # length in UTF-16 units
        end = self.data.index(b"\0", offset)
        return self.data[offset:end].decode("utf-8", "replace")

    def type_name(self, index):
        return self.string(self.u4(self.type_ids + 4 * index))

    def method(self, index):
        _, proto, name = struct.unpack_from("<HHI", self.data, self.method_ids + 8 * index)
        shorty = self.string(self.u4(self.proto_ids + 12 * proto))
        return self.string(name), shorty

    def direct_methods(self):
        """Each class defined here, with the (name, shorty) of its direct methods."""
        for k in range(self.class_count):
            entry = self.class_defs + 32 * k
            name = self.type_name(self.u4(entry))
            class_data = self.u4(entry + 24)
            methods = []
            if class_data:
                offset = class_data
                static_fields, offset = uleb128(self.data, offset)
                instance_fields, offset = uleb128(self.data, offset)
                direct, offset = uleb128(self.data, offset)
                _, offset = uleb128(self.data, offset)
                for _ in range(static_fields + instance_fields):
                    _, offset = uleb128(self.data, offset)
                    _, offset = uleb128(self.data, offset)
                index = 0
                for _ in range(direct):
                    diff, offset = uleb128(self.data, offset)
                    _, offset = uleb128(self.data, offset)  # access flags
                    _, offset = uleb128(self.data, offset)  # code offset
                    index += diff
                    methods.append(self.method(index))
            yield name, methods


def main(bundle):
    found = {}
    with zipfile.ZipFile(bundle) as zf:
        dex_files = [n for n in zf.namelist() if n.startswith("base/dex/") and n.endswith(".dex")]
        for name in dex_files:
            for cls, methods in Dex(zf.read(name)).direct_methods():
                if cls in NEEDS_CONSTRUCTOR:
                    found[cls] = methods
    if not dex_files:
        print(f"::error::No dex files in {bundle}.")
        return 1
    failed = False
    for cls, why in NEEDS_CONSTRUCTOR.items():
        if cls not in found:
            print(f"::error::{cls} is missing from the release build. {why}.")
            failed = True
        elif ("<init>", "V") not in found[cls]:
            print(f"::error::R8 removed the constructor of {cls}, so the game crashes on start. {why}.")
            failed = True
        else:
            print(f"{cls}: constructor kept")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
