# To convert sm file written by professionalist
# to compare later on with algorithm there for matching accuracy

# Open input file
input_filename = "Abracadabra.sm"
output_filename = "AbracadabraOut.sm"

with open(input_filename, "r") as f:
    lines = f.readlines()

output_lines = []
current_block = []

first_trigger = False
second_trigger = False
parse_mode = False

for line in lines:
    line = line.strip()
    if parse_mode:
        if line == "," or line == ";":  # End of a block
            # Process current block: add '.' to each line
            index = 0
            for l in current_block:
                # print (f"Block fragment: {l} len: {len(current_block)} index: {index % 2 == 0}")
                if len(current_block) == 16:
                    output_lines.append(l)
                elif len(current_block) == 4:
                    output_lines.append(l)
                    output_lines.append("0000")
                    output_lines.append("0000")
                    output_lines.append("0000")
                elif len(current_block) == 8:
                    output_lines.append(l)
                    output_lines.append("0000")
                index+=1
            # You can uncomment it to add also "," lines
            # output_lines.append(line)
            # for l in output_lines:
            #     print (f"Output fragment: {l}")
            current_block = []
        elif len(line) == 4:  # Skip empty lines
            current_block.append(line)
        elif "dance-single" in line or "dance-double" in line:
            first_trigger = False
            second_trigger = False
            parse_mode = False
            break
        else:
            output_lines.append(line)
    else:
        if "dance-single:" in line:
            first_trigger = True
        elif "Hard:" in line:
            second_trigger = True
        if first_trigger and second_trigger:
            parse_mode = True

# Save to new file
with open(output_filename, "w") as f:
    f.write("\n".join(output_lines))

print(f"Processed file saved as {output_filename}")
