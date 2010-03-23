/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/** 
 * @author Intel, Evgueni Brevnov, Ivan Volosyuk
 */  


#ifndef _M2N_IPF_INTERNAL_H_
#define _M2N_IPF_INTERNAL_H_

// This file describes the internal IPF interface of m2n frames.
// It can be used by stubs to generate code to push and pop m2n frames and to update object handles fields.
// It is also used by stack iterators.

#include "Code_Emitter.h"
#include "m2n.h"
#include "open/types.h"

// Generate code to push an M2nFrame onto the stack
// The activation frame of the stub is used for the M2nFrame as are certain registers in this frame.
// The stub must preserve all preserved registers including pfs, gp, and b0 from entry to the stub to the time of push_m2n.
// The stub may use up to 8 inputs, the requested number of locals, and the requested number of outputs after push_m2n.
// method: the method to be associated with the M2nFrame or NULL for no association
// handles: does the stub want local handles or not
// num_on_stack: this number plus the current sp is the sp at entry to the stub (should be positive as stack grows down)
// num_local: the number of local registers above the M2N registers required by the stub
// num_outputs: the number of output registers required by the stub
// do_alloc: if false, the function should assume that the stacked register
//           frame has been allocated, and no alloc instruction needs to be executed; it should also assume that ar.pfs is already saved at its proper place
// return: the register number for the first local, the outputs immediately follow the locals
// Note, the stub may use some of the 8 inputs as locals if it wants to
unsigned m2n_gen_push_m2n(Merced_Code_Emitter*, Method_Handle method, frame_type current_frame_type, bool handles, unsigned num_on_stack, unsigned num_locals,
                          unsigned num_outputs, bool do_alloc=true);

enum M2nPreserveRet { MPR_None, MPR_Gr, MPR_Fr };

// Generate code to pop the M2nFrame from the stack.
// This should be matched by a preceeding push_m2n in the stub.
// handles: should match the push_m2n handles argument, if true the generated code will free the handles.
// preserve_ret_regs: the number of return registers to preserve (starting with r8).
// Note that the pop restores the callee saves gp registers, pfs, gp, and b0 to the values that had at the push m2n; it does not restore sp.
// do_alloc: must have the same value as the corresponding m2n_gen_push_m2n() parameter
// target: if handles==true and the vm property vm.free_local_object_handles
//   is also true, m2n_gen_pop_m2n will need to set a target in the emitter;
//   target will be the number used.  Otherwise, this parameter is ignored.
// out_reg: if handles==true and the vm property vm.free_local_object_handles
//   is also true, m2n_gen_pop_m2n needs to know the first output register; out_reg is this register.  Otherwise this parameter is ignored
void m2n_gen_pop_m2n(Merced_Code_Emitter*, bool handles, M2nPreserveRet preserve_ret_regs, bool do_alloc=true, unsigned out_reg=0, int target=-1);

// Generate code to set the local handles of the M2nFrame that is also the current frame.
// Preserves all registers that are not used to store M2nFrame information.
void m2n_gen_set_local_handles(Merced_Code_Emitter*, unsigned src_reg);

// Generate code to set the local handles of the M2nFrame that is also the current frame.
// Preserves all registers that are not used to store M2nFrame information.
void m2n_gen_set_local_handles_imm(Merced_Code_Emitter*, uint64 imm_val);

// Generate code to save additional preserved registers not normally saved by push_m2n.
// The combination of push_m2n and save_extra_preserved_registers will save all preserved registers as needed by exception propogation.
// The code generated by this function must follow that of push_m2n.
// Note that this function uses the memory stack, expects the scratch area above sp, and leaves a scratch area above sp.
void m2n_gen_save_extra_preserved_registers(Merced_Code_Emitter* emitter);


// returns the number of the last GR that the M2N frame uses
unsigned m2n_get_last_m2n_reg();

// the following functions return the GR numbers where various things should be saved
unsigned m2n_get_pfs_save_reg();
unsigned m2n_get_return_save_reg();
unsigned m2n_get_gp_save_reg();

// The IPF calling convention defines how to layout the arguments into words and then how to place
// these into gp registers, fp registers, or memory stack.  This function returns a pointer to the
// nth word assuming it is either in a gp register or on the memory stack.
uint64* m2n_get_arg_word(M2nFrame*, unsigned n);

//////////////////////////////////////////////////////////////////////////
// Implementation details

// An M2nFrame is always represented using the bsp value for register 32 of the frame
// The information needed for the frame is stored in stacked local registers or on the memory stack.
// It can be accessed by computing the spill location from the bsp value or by retrieving the sp value and 

uint64* m2n_get_bsp(M2nFrame*);
uint64* m2n_get_extra_saved(M2nFrame*);

// Flushes register stack of the current thread into backing store and calls target procedure.
NativeCodePtr m2n_gen_flush_and_call();

// An M2nFrame will always have 8 input registers, some local stacked registers to save stuff, and some outputs

#define M2N_NUMBER_ALIGNS            2
#define M2N_NUMBER_INPUTS            8
#define M2N_NUMBER_LOCALS            17

// The following registers are used in M2nFrames to hold the indicated values
// The register numbers must be distinct, at least 40 (so they don't conflict with inputs), and less than 40+M2N_NUMBER_LOCALS

#define M2N_SAVED_PFS                40
#define M2N_SAVED_RETURN_ADDRESS     41
#define M2N_SAVED_M2NFL              42
#define M2N_SAVED_SP                 43
#define M2N_SAVED_GP                 44
#define M2N_SAVED_PR                 45
#define M2N_SAVED_UNAT               46
#define M2N_SAVED_R4                 47
#define M2N_SAVED_R5                 48
#define M2N_SAVED_R6                 49
#define M2N_SAVED_R7                 50
#define M2N_EXTRA_SAVED_PTR          51
#define M2N_OBJECT_HANDLES           52
#define M2N_METHOD                   53
#define M2N_FRAME_TYPE               54
#define M2N_EXTRA_RNAT               55
// this must be last register
#define M2N_EXTRA_UNAT               56

// Only the callee saves general registers are normally saved in the M2nFrame along with special things like pfs, return address, etc.
// The full set of preserved registers includes callee saves floating point and branch registers as well.
// These are saved, if requested, onto the memory stack as follows:
//                  +-------------------------+
//                  | Saved f2                |
// Extra Saved ---> +-------------------------+
//                  | Saved f3..f5            |
//                  +-------------------------+
//                  | Saved f16..f31          |
//                  +-------------------------+
//                  | Scratch area (8 bytes)  |
//                  +-------------------------+
//                  | Saved b1..b5            |
//                  +-------------------------+
//                  | Saved ar.fpsr           |
//                  +-------------------------+
//                  | Saved ar.unat           |
//                  +-------------------------+
//                  | Saved ar.lc             |
//                  +-------------------------+

#define M2N_EXTRA_SAVES_SPACE 400

#ifdef _EM64T_
#error Should not be included!
#endif

struct M2nFrame {
    union {
        uint64 buff[M2N_NUMBER_INPUTS + M2N_NUMBER_LOCALS + M2N_NUMBER_ALIGNS];
        struct {
            M2nFrame*            prev_m2nf;
            ObjectHandles*       local_object_handles;
            Method_Handle        method;
            frame_type           current_frame_type; // type of the current frame also shows is the frame unwindable
        };
    };
};

#endif //!_M2N_IPF_INTERNAL_H_